from flask import Flask, request, jsonify
import numpy as np
import tensorflow as tf
from PIL import Image, ImageEnhance
import os
import cv2
from pymongo import MongoClient
from bson import ObjectId
from datetime import datetime

# Initialize the Flask app
app = Flask(__name__)

# MongoDB configuration
MONGO_URI = "mongodb://localhost:27017"
DATABASE_NAME = "fruit_veg_calorie_db"
COLLECTION_NAME = "predictions"

# Initialize MongoDB connection
try:
    client = MongoClient(MONGO_URI)
    db = client[DATABASE_NAME]
    collection = db[COLLECTION_NAME]
    # Test the connection
    client.admin.command('ping')
    print("Successfully connected to MongoDB!")
except Exception as e:
    print(f"Failed to connect to MongoDB: {e}")
    client = None
    db = None
    collection = None

# Load your custom-trained model (ensure it's in the correct path)
model = tf.keras.models.load_model('./models/custom_mobilenet_model.h5', compile=False)

# Define your custom class labels
class_labels = [
    'apple', 'banana', 'beetroot', 'bell pepper', 'cabbage', 'capsicum', 'carrot',
    'cauliflower', 'chilli pepper', 'corn', 'cucumber', 'eggplant', 'garlic', 'ginger',
    'grapes', 'jalepeno', 'kiwi', 'lemon', 'lettuce', 'mango', 'onion', 'orange',
    'paprika', 'pear', 'peas', 'pineapple', 'pomegranate', 'potato', 'raddish',
    'soy beans', 'spinach', 'sweetcorn', 'sweetpotato', 'tomato', 'turnip', 'watermelon'
]

calories_per_100g = {
    'apple': 52,
    'banana': 89,
    'beetroot': 43,
    'bell pepper': 31,
    'cabbage': 25,
    'capsicum': 31,
    'carrot': 41,
    'cauliflower': 25,
    'chilli pepper': 40,
    'corn': 86,
    'cucumber': 16,
    'eggplant': 25,
    'garlic': 149,
    'ginger': 80,
    'grapes': 69,
    'jalepeno': 29,
    'kiwi': 61,
    'lemon': 29,
    'lettuce': 15,
    'mango': 60,
    'onion': 40,
    'orange': 47,
    'paprika': 282,
    'pear': 57,
    'peas': 81,
    'pineapple': 50,
    'pomegranate': 83,
    'potato': 77,
    'raddish': 16,
    'soy beans': 446,
    'spinach': 23,
    'sweetcorn': 86,
    'sweetpotato': 86,
    'tomato': 18,
    'turnip': 28,
    'watermelon': 30
}

# Preprocess the image for MobileNetV2
def preprocess_image(img_path):
    img = Image.open(img_path).convert('RGB')
    img_np = np.array(img)

    gray = cv2.cvtColor(img_np, cv2.COLOR_RGB2GRAY)
    _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if contours:
        # Sort contours by area (largest first)
        contours = sorted(contours, key=cv2.contourArea, reverse=True)

        # Take top 2 contours
        top_contours = contours[:2]

        # Get bounding boxes for them
        boxes = [cv2.boundingRect(c) for c in top_contours]

        # Merge bounding boxes into one that covers both objects
        x_min = min([x for (x, y, w, h) in boxes])
        y_min = min([y for (x, y, w, h) in boxes])
        x_max = max([x + w for (x, y, w, h) in boxes])
        y_max = max([y + h for (x, y, w, h) in boxes])

        img_np = img_np[y_min:y_max, x_min:x_max]


    img = Image.fromarray(img_np)

    img = img.resize((224, 224))  # Resize the image to 224x224

    img = adaptive_enhancement(img)
    img = adaptive_sharpness(img)

    
    img_array = np.array(img)  # Convert image to numpy array
    img_array = np.expand_dims(img_array, axis=0)  # Add batch dimension

    # Normalize the image to [-1, 1] as expected by MobileNetV2
    img_array = img_array / 255.0  # Scale to [0, 1]
    img_array = img_array - 0.5    # Center to [-0.5, 0.5]
    img_array = img_array * 2      # Scale to [-1, 1]

    return img_array

def adaptive_enhancement(img):
    img_gray = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2GRAY)
    contrast_score = img_gray.std()  # measure contrast
    
    if contrast_score < 40:  
        enhancer = ImageEnhance.Contrast(img)
        img = enhancer.enhance(1.2)
    return img

def adaptive_sharpness(img):
    img_gray = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2GRAY)
    sharpness_score = cv2.Laplacian(img_gray, cv2.CV_64F).var()
    
    if sharpness_score < 100:
        img = ImageEnhance.Sharpness(img).enhance(1.3)
    return img


# Function to classify the image and return top prediction
def classify_image(img_path):
    img_array = preprocess_image(img_path)
    predictions = model.predict(img_array)
    top_index = predictions[0].argmax()
    top_prediction = (class_labels[top_index], float(predictions[0][top_index]))
    return top_prediction

# Route for image classification
@app.route('/predict', methods=['POST'])
def predict():
    try:
        if 'file' not in request.files:
            return jsonify({'error': 'No file part'}), 400

        file = request.files['file']

        if file.filename == '':
            return jsonify({'error': 'No selected file'}), 400

        # Save uploaded file
        if not os.path.exists('uploads'):
            os.makedirs('uploads')
        img_path = os.path.join('uploads', file.filename)
        file.save(img_path)

        label, score = classify_image(img_path)

        # Optional: remove the uploaded file to save space
        os.remove(img_path)

        # Get calories
        calories = calories_per_100g.get(label, "Unknown")

        response = {
            'prediction': {'label': label, 'probability': round(score, 2), "calories_per_100g": calories},
            'timestamp': datetime.now().isoformat(),
            'filename': file.filename
        }
        
        # Save to MongoDB only if probability is 80% or higher
        if collection is not None and score >= 0.8:
            try:
                result = collection.insert_one(response.copy())
                response['_id'] = str(result.inserted_id)
                print(f"Prediction saved to MongoDB with ID: {result.inserted_id}")
            except Exception as e:
                print(f"Failed to save to MongoDB: {e}")
                # Continue even if MongoDB save fails
        elif score < 0.8:
            print(f"Prediction not saved - probability {round(score, 2)} is below 80% threshold")
        
        print(response)
        return jsonify(response)

    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Route to get all saved predictions
@app.route('/predictions', methods=['GET'])
def get_all_predictions():
    try:
        if collection is None:
            return jsonify({'error': 'MongoDB connection not available'}), 500
        
        # Get all predictions sorted by timestamp (newest first)
        predictions = list(collection.find({}).sort('timestamp', -1))
        
        # Convert ObjectId to string for JSON serialization
        for prediction in predictions:
            prediction['_id'] = str(prediction['_id'])
        
        response = {
            'predictions': predictions,
            'total_count': len(predictions)
        }
        
        return jsonify(response)
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Route to delete a prediction by ID
@app.route('/predictions/<prediction_id>', methods=['DELETE'])
def delete_prediction(prediction_id):
    try:
        if collection is None:
            return jsonify({'error': 'MongoDB connection not available'}), 500
        
        # Validate ObjectId format
        try:
            obj_id = ObjectId(prediction_id)
        except:
            return jsonify({'error': 'Invalid prediction ID format'}), 400
        
        result = collection.delete_one({'_id': obj_id})
        
        if result.deleted_count > 0:
            return jsonify({'message': 'Prediction deleted successfully'})
        else:
            return jsonify({'error': 'Prediction not found'}), 404
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Run the Flask app
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
