# Fruit and Vegetable Calorie Counter

This application is designed to classify and estimate the calorie content of fruits and vegetables through image recognition using a custom-trained MobileNet model. The app is powered by Flask for backend processing with MongoDB for data storage, and Streamlit for a user-friendly interface.

## Project Structure

- **models/**: Contains the custom-trained MobileNet model used for classifying fruits and vegetables.
- **uploads/**: Directory where test images are uploaded for classification.
- **app.py**: Flask backend that serves the model and handles classification requests. Run using `python app.py`.
- **main.py**: Streamlit app that provides a simple UI for users to interact with the classification system. Run using `streamlit run main.py`.
- **requirements.txt**: List of Python dependencies for the project.

## Features

- **Flask Backend**: The backend API is built using Flask and handles image classification requests. It uses the custom-trained MobileNet model to predict the calorie content of fruits and vegetables.
- **MongoDB Integration**: All predictions are automatically saved to MongoDB with timestamps and metadata.
- **Streamlit UI**: The front-end interface is built using Streamlit, allowing users to upload images and receive calorie estimations directly.
- **REST API**: Complete API endpoints for mobile app integration.

## Installation

To run this application locally, follow these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/chathuryaprasad/Fruit_Veg_Calorie_Counter.git
2. Navigate to the project directory:
   ```bash
   cd Fruit_Veg_Calorie_Counter
3. Install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## MongoDB Setup

This application requires MongoDB to store prediction data. Choose one of the following options:

### Quick Setup (Recommended)
1. **Download MongoDB Community Server** from [mongodb.com](https://www.mongodb.com/try/download/community)
2. **Install with default settings** (make sure "Install as Service" is checked)
3. **Start MongoDB service**:
   ```bash
   net start MongoDB
   ```
4. **Verify connection**: MongoDB will run on `mongodb://localhost:27017`

### Alternative Options
- **MongoDB Atlas** (Cloud): Create free account at [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
- **Docker**: `docker run -d -p 27017:27017 --name mongodb mongo:latest`

> **Note**: The application will automatically create the database (`fruit_veg_calorie_db`) and collection (`predictions`) when first run.

For detailed setup instructions, see `MONGODB_SETUP.md`

## Usage
1. Run Flask API (Backend): 
To start the Flask backend, run the following command:
    ```bash
    python app.py
This will start the Flask server and the model will be ready to receive classification requests.

2. To start streamlit web application, run the following command:
    ```bash
    streamlit run main.py
This will open the Streamlit app in your web browser where you can upload an image of a fruit or vegetable, and the app will predict the calorie content.

## API Endpoints

The Flask backend provides the following REST API endpoints:

### Image Classification & Storage
- **POST** `/predict` - Upload image for classification (automatically saves to MongoDB)

### Data Retrieval 
- **GET** `/predictions` - Get all saved predictions (no pagination)
- **GET** `/predictions/{id}` - Get specific prediction by ID
- **DELETE** `/predictions/{id}` - Delete a prediction

### Example API Usage
```bash
# Classify an image
curl -X POST -F "file=@apple.jpg" http://localhost:5000/predict

# Get all predictions
curl http://localhost:5000/predictions

# Get specific prediction
curl http://localhost:5000/predictions/64f1a2b3c4d5e6f7g8h9i0j1
```

For complete API documentation, see `API_DOCUMENTATION.md`

## Model Details

The classification model used in this project is a custom-trained MobileNet model, designed for efficient performance on mobile and web platforms. 
It is trained on a dataset of fruits and vegetables, with labels corresponding to various types of produce.

## Flask API Testing (via Postman)

To upload an image file for classification via the Flask API, follow these steps in Postman:

1. **Set the Request URL**:
   - Use the URL `http://127.0.0.1:5000/predict` for the Flask backend.

2. **Set the Request Method**:
   - Choose **POST** as the method.

3. **Set up the Body**:
   - Go to the **Body** tab in Postman.
   - Select **form-data**.
   - For the **Key**, type `file` (this is the key Flask expects for image uploads).
   - For the **Value**, click **Select Files** and choose the image you want to upload.

4. **Send the Request**:
   - Click the **Send** button to submit the image.
   - The response will return predictions based on the image provided.

![image](https://github.com/user-attachments/assets/6f1ab84c-0f49-449b-a46a-6d9ca8aa16d4)
