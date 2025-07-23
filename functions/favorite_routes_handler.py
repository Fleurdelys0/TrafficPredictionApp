import firebase_admin
from firebase_admin import credentials, firestore, messaging
import requests
import os
from datetime import datetime
from firebase_functions import scheduler_fn

db = None

is_local_cli_analysis = (os.environ.get('FIREBASE_CLI_PREVIEWS') == 'true' or \
                         os.environ.get('FUNCTIONS_EMULATOR') == 'true')

if not is_local_cli_analysis:
    try:
        if not firebase_admin._apps:
            print("Attempting to initialize Firebase Admin SDK for deployed/emulated environment (in favorite_routes_handler)...")
            firebase_admin.initialize_app()
            print("Firebase Admin SDK initialized for deployed/emulated environment (in favorite_routes_handler).")
        else:
            firebase_admin.get_app()
            print("Firebase Admin SDK already initialized, using default app (in favorite_routes_handler).")
        db = firestore.client()
    except Exception as e:
        print(f"CRITICAL ERROR during Firebase Admin SDK initialization in favorite_routes_handler: {e}")
else:
    print("Skipping Firebase Admin SDK initialization for local Firebase CLI analysis phase (in favorite_routes_handler).")

GOOGLE_MAPS_API_KEY = os.environ.get('GOOGLE_MAPS_API_KEY')
TRAFFIC_DELAY_THRESHOLD_PERCENTAGE = int(os.environ.get('TRAFFIC_DELAY_THRESHOLD_PERCENTAGE', 1))


def _send_fcm_notification_internal(token, title, body, route_name, route_id):
    """Internal helper to send FCM message."""
    if db is None:
        print("Firestore client (db) is not initialized. FCM send might be affected if it needed project context implicitly.")
    
    print(f"Attempting to send FCM to token {token[:20]}... for route: {route_name}")
    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data={
                'type': 'traffic_alert',
                'routeName': route_name,
                'routeId': route_id,
            },
            token=token,
        )
        response = messaging.send(message)
        print(f"Successfully sent FCM message: {response}")
    except Exception as e:
        print(f"Error sending FCM message: {e}")

def _check_all_favorite_routes_traffic_logic():
    """
    The core logic for checking all users' favorite routes for traffic.
    """
    if db is None:
        print("ERROR: Firestore client (db) is not initialized in _check_all_favorite_routes_traffic_logic. Skipping core logic.")
        return

    print(f"Core logic: _check_all_favorite_routes_traffic_logic started at {datetime.utcnow()}.")
    
    if GOOGLE_MAPS_API_KEY == 'YOUR_GOOGLE_MAPS_API_KEY_HERE_REPLACE_ME' or not GOOGLE_MAPS_API_KEY:
        print("ERROR: GOOGLE_MAPS_API_KEY is not configured. Exiting logic.")
        return

    users_ref = db.collection('users')
    try:
        users = users_ref.stream()
    except Exception as e:
        print(f"Error streaming users from Firestore: {e}")
        return

    user_count = 0
    for user_doc in users:
        user_count += 1
        user_id = user_doc.id
        user_data = user_doc.to_dict()
        if not user_data:
            print(f"User document for {user_id} is empty or non-existent. Skipping.")
            continue
        fcm_token = user_data.get('fcmToken')

        if not fcm_token:
            print(f"User {user_id} has no FCM token. Skipping.")
            continue

        favorite_routes_ref = users_ref.document(user_id).collection('favoriteRoutes')
        try:
            favorite_routes = favorite_routes_ref.stream()
        except Exception as e:
            print(f"Error streaming favorite routes for user {user_id}: {e}")
            continue

        print(f"Checking routes for user: {user_id}")
        route_count_for_user = 0
        for route_doc in favorite_routes:
            route_count_for_user +=1
            route_data = route_doc.to_dict()
            if not route_data:
                print(f"Empty route document found for user {user_id}. Skipping.")
                continue

            route_name = route_data.get('name', 'Favorite Route')
            origin_lat = route_data.get('originLat')
            origin_lng = route_data.get('originLng')
            dest_lat = route_data.get('destinationLat')
            dest_lng = route_data.get('destinationLng')

            if not all(isinstance(coord, (int, float)) for coord in [origin_lat, origin_lng, dest_lat, dest_lng]):
                print(f"Route '{route_name}' for user {user_id} has invalid or incomplete coordinates. Skipping. Data: {route_data}")
                continue
            
            origin_coords = f"{origin_lat},{origin_lng}"
            dest_coords = f"{dest_lat},{dest_lng}"
            
            print(f"  Checking traffic for route: '{route_name}' ({origin_coords} to {dest_coords})")
            try:
                directions_url = (
                    f"https://maps.googleapis.com/maps/api/directions/json?"
                    f"origin={origin_coords}&destination={dest_coords}"
                    f"&departure_time=now&traffic_model=best_guess"
                    f"&fields=routes/legs/duration,routes/legs/duration_in_traffic,status,error_message"
                    f"&key={GOOGLE_MAPS_API_KEY}"
                )
                api_response = requests.get(directions_url, timeout=10)
                api_response.raise_for_status()
                directions_data = api_response.json()

                if directions_data.get('status') == 'OK' and directions_data.get('routes'):
                    legs = directions_data['routes'][0].get('legs')
                    if legs:
                        route_info = legs[0]
                        duration_in_traffic_data = route_info.get('duration_in_traffic', {})
                        duration_data = route_info.get('duration', {})
                        duration_in_traffic_sec = duration_in_traffic_data.get('value')
                        duration_sec = duration_data.get('value')

                        if duration_in_traffic_sec is not None and duration_sec is not None:
                            delay_seconds = duration_in_traffic_sec - duration_sec
                            delay_minutes = round(delay_seconds / 60)
                            delay_percentage = (delay_seconds / duration_sec) * 100 if duration_sec > 0 else 0
                            
                            print(f"  Route '{route_name}': Typical: {duration_sec}s, In Traffic: {duration_in_traffic_sec}s, Delay: {delay_minutes} min ({delay_percentage:.1f}%)")
                            if delay_percentage >= TRAFFIC_DELAY_THRESHOLD_PERCENTAGE and delay_minutes >= 1:
                                message_title = f"Traffic Alert: {route_name}"
                                message_body = (
                                    f"Heavy traffic on '{route_name}'. "
                                    f"Travel time is ~{round(duration_in_traffic_sec / 60)} min "
                                    f"({delay_minutes} min delay)."
                                )
                                _send_fcm_notification_internal(fcm_token, message_title, message_body, route_name, route_doc.id)
                        else:
                            print(f"  Could not get duration/duration_in_traffic values for route '{route_name}'. Data: {route_info}")
                    else:
                        print(f"  No 'legs' found in Directions API response for route '{route_name}'.")
                else:
                    print(f"  Directions API error for route '{route_name}': Status: {directions_data.get('status')}, Error: {directions_data.get('error_message')}")
            except requests.exceptions.Timeout:
                print(f"  Timeout calling Directions API for route '{route_name}'.")
            except requests.exceptions.RequestException as e:
                print(f"  Error calling Directions API for route '{route_name}': {e}")
            except Exception as e:
                print(f"  Unexpected error processing route '{route_name}': {e}")

        print(f"  Finished {route_count_for_user} routes for user {user_id}.")
    print(f"Core logic: _check_all_favorite_routes_traffic_logic finished. Processed {user_count} users.")


@scheduler_fn.on_schedule(schedule="0 * * * *", timezone=scheduler_fn.Timezone("Europe/Istanbul"))
def hourly_traffic_check_scheduler(event: scheduler_fn.ScheduledEvent) -> None:
    print(f"Hourly traffic check triggered by Firebase Scheduler: {event.job_name}, {event.schedule_time}")
    _check_all_favorite_routes_traffic_logic()
