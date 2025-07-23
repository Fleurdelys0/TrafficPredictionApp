import json
import joblib
import pandas as pd
import numpy as np
import os
import sys
import traceback
import math
from firebase_functions import https_fn, options
from google.cloud import storage

try:
    from favorite_routes_handler import hourly_traffic_check_scheduler
    print("Successfully imported hourly_traffic_check_scheduler from favorite_routes_handler.")
except ImportError:
    print("Could not import hourly_traffic_check_scheduler. Ensure favorite_routes_handler.py exists and the function is defined.")
    hourly_traffic_check_scheduler = None

EXPECTED_API_KEY = os.environ.get("MY_TRAFFIC_API_KEY", "ENTER_API")

storage_client = None
model = None
MODEL_BUCKET_NAME = os.environ.get("MODEL_BUCKET_NAME", "ENTER_BUCKET_NAME") 
MODEL_BLOB_NAME = 'traffic_model.joblib'
LOCAL_MODEL_PATH = '/tmp/traffic_model.joblib'

def download_model_if_not_exists():
    global model, storage_client
    if model is not None:
        return model

    if not os.path.exists(LOCAL_MODEL_PATH):
        print(f"Model not found locally at {LOCAL_MODEL_PATH}. Downloading from GCS bucket '{MODEL_BUCKET_NAME}', blob '{MODEL_BLOB_NAME}'...")
        if MODEL_BUCKET_NAME == "YOUR_GCS_BUCKET_NAME_HERE":
            print("!!! ERROR: MODEL_BUCKET_NAME is not configured. Cannot download model.")
            raise ValueError("MODEL_BUCKET_NAME is not configured.")
            
        if storage_client is None:
            storage_client = storage.Client()
        try:
            bucket = storage_client.bucket(MODEL_BUCKET_NAME)
            blob = bucket.blob(MODEL_BLOB_NAME)
            os.makedirs(os.path.dirname(LOCAL_MODEL_PATH), exist_ok=True)
            blob.download_to_filename(LOCAL_MODEL_PATH)
            print(f"Model downloaded to {LOCAL_MODEL_PATH}")
        except Exception as e:
            print(f"!!! ERROR downloading model from GCS: {e}")
            traceback.print_exc(file=sys.stderr)
            raise 
    
    try:
        print(f"Loading model from {LOCAL_MODEL_PATH}...")
        model = joblib.load(LOCAL_MODEL_PATH)
        print("ML Model loaded successfully.")
    except Exception as e:
        print(f"!!! ERROR loading ML Model from {LOCAL_MODEL_PATH}: {e}")
        traceback.print_exc(file=sys.stderr)
        raise
    return model

def haversine_distance(lat1, lon1, lat2, lon2):
    R = 6371
    lat1_rad, lon1_rad, lat2_rad, lon2_rad = map(math.radians, [lat1, lon1, lat2, lon2])
    dlon = lon2_rad - lon1_rad
    dlat = lat2_rad - lat1_rad
    a = math.sin(dlat / 2)**2 + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

@https_fn.on_request(
    cors=options.CorsOptions(cors_origins=["*"], cors_methods=["get", "post"]),
    memory=options.MemoryOption.MB_512
)
def get_traffic_prediction(req: https_fn.Request) -> https_fn.Response:
    global model
    print("--- get_traffic_prediction function entered ---", file=sys.stderr)

    try:
         api_key = req.headers.get("x-api-key")
         if api_key != EXPECTED_API_KEY:
             print("Error: Invalid API Key received.", file=sys.stderr)
             return https_fn.Response("Unauthorized: Invalid API Key", status=401, mimetype="text/plain")
         print("API Key validated.", file=sys.stderr)
    except Exception as e:
        print(f"Error checking API Key: {e}", file=sys.stderr)
        return https_fn.Response(json.dumps({"error": "Internal Server Error during API Key check"}), status=500, mimetype="application/json")

    try:
        current_model = download_model_if_not_exists()
        if current_model is None:
             print("Error: Model is None after load attempt.", file=sys.stderr)
             return https_fn.Response(json.dumps({"error": "Internal Server Error: Model could not be initialized"}), status=500, mimetype="application/json")
    except Exception as e:
        print(f"!!! ERROR initializing/loading ML Model: {e}", file=sys.stderr)
        traceback.print_exc(file=sys.stderr)
        return https_fn.Response(json.dumps({"error": f"Internal Server Error: Could not load ML model. Details: {str(e)}"}), status=500, mimetype="application/json")

    try:
        from_x = float(req.args.get("from_x"))
        from_y = float(req.args.get("from_y"))
        to_x = float(req.args.get("to_x"))
        to_y = float(req.args.get("to_y"))
        time = int(req.args.get("time"))
        is_weekday = int(req.args.get("is_weekday"))
        print(f"Received parameters: from_x={from_x}, from_y={from_y}, to_x={to_x}, to_y={to_y}, time={time}, is_weekday={is_weekday}", file=sys.stderr)

        if time not in [8, 14, 20]:
            raise ValueError("Invalid 'time' parameter. Must be 8, 14, or 20.")
        if is_weekday not in [0, 1]:
             raise ValueError("Invalid 'is_weekday' parameter. Must be 0 or 1.")
        print("Parameters validated.", file=sys.stderr)
    except (TypeError, ValueError, AttributeError) as e:
        print(f"Error processing input parameters: {e}", file=sys.stderr)
        return https_fn.Response(f"Bad Request: Invalid or missing parameters. Error: {e}", status=400, mimetype="text/plain")
    except Exception as e:
         print(f"Unexpected error processing parameters: {e}", file=sys.stderr)
         return https_fn.Response(json.dumps({"error": f"Internal Server Error during parameter processing: {str(e)}"}), status=500, mimetype="application/json")

    try:
        feature_order = ['from_x', 'from_y', 'to_x', 'to_y', 'time', 'is_weekday']
        input_data = pd.DataFrame([[from_x, from_y, to_x, to_y, time, is_weekday]], columns=feature_order)
        print("Input DataFrame for prediction:\n", input_data.to_string(), file=sys.stderr)

        prediction = current_model.predict(input_data)
        predicted_speed_kmh = float(prediction[0])
        print(f"Prediction successful. Predicted speed: {predicted_speed_kmh} km/h", file=sys.stderr)

        segment_distance_km = haversine_distance(from_y, from_x, to_y, to_x)
        print(f"Calculated segment distance: {segment_distance_km:.2f} km", file=sys.stderr)

        if predicted_speed_kmh > 0.1:
            travel_time_hours = segment_distance_km / predicted_speed_kmh
            estimated_travel_time_minutes = travel_time_hours * 60
            print(f"Estimated travel time: {estimated_travel_time_minutes:.2f} minutes", file=sys.stderr)
        else:
            estimated_travel_time_minutes = float('inf')
            print("Warning: Predicted speed is too low, travel time is effectively infinite or undefined.", file=sys.stderr)

        if predicted_speed_kmh < 30:
            traffic_condition = "Heavy"
        elif predicted_speed_kmh < 50:
            traffic_condition = "Moderate"
        else:
            traffic_condition = "Light"
    except Exception as e:
        print(f"!!! Error during prediction or time calculation: {e}", file=sys.stderr)
        traceback.print_exc(file=sys.stderr)
        return https_fn.Response(json.dumps({"error": f"Prediction or time calculation failed: {str(e)}"}), status=500, mimetype="application/json")

    try:
        response_data = {
            "requested_params": {
                 "from_x": from_x, "from_y": from_y, "to_x": to_x, "to_y": to_y,
                 "time": time, "is_weekday": is_weekday
            },
            "predicted_speed_kmh": predicted_speed_kmh,
            "estimated_condition": traffic_condition,
            "segment_distance_km": segment_distance_km,
            "estimated_travel_time_minutes": estimated_travel_time_minutes,
            "source": "ML Model (RandomForest with Time Est. from GCS)"
        }
        if math.isinf(estimated_travel_time_minutes) or math.isnan(estimated_travel_time_minutes):
            response_data["estimated_travel_time_minutes"] = None

        json_response_data = json.dumps(response_data)
        print("Sending successful response:", json_response_data, file=sys.stderr)
        return https_fn.Response(response=json_response_data, status=200, mimetype="application/json")
    except Exception as e:
         print(f"Error preparing final response: {e}", file=sys.stderr)
         return https_fn.Response(json.dumps({"error": f"Internal Server Error preparing response: {str(e)}"}), status=500, mimetype="application/json")
