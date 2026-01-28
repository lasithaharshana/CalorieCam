import streamlit as st
import requests

st.set_page_config(
    page_title="Fruit & Veg Calories Counter",
    page_icon="🥦",
    layout="centered",
    initial_sidebar_state="expanded"
)

# Title
st.title("🍎 Fruit & Veg Calories Counter")

st.write(
    "Upload an image of a fruit or vegetable, and the model will predict its label and confidence."
)

# File uploader
uploaded_file = st.file_uploader("Upload an image", type=["jpg", "png"], key="uploaded_file")

# Function to check the API response
def check_api_response(image):
    url = "http://127.0.0.1:5000/predict"  # Your API endpoint
    files = {"file": image}
    try:
        response = requests.post(url, files=files)
        response.raise_for_status()  # Raise an exception for HTTP errors
        return response.json(), response.status_code
    except requests.exceptions.HTTPError as http_err:
        st.error(f"HTTP error occurred: {http_err}")  # Specific HTTP error
        return None, 500
    except Exception as err:
        st.error(f"Other error occurred: {err}")  # General error
        return None, 500

# Check the API response if an image is uploaded
if uploaded_file is not None:
    # Check the API response and status code
    with st.spinner("Classifying image..."):
        json_response, status_code = check_api_response(uploaded_file)
    
    if json_response is not None:
        # Display the status code
        # st.write(f"API Status Code: {status_code}")
        
        # If status code is 200, display the label, otherwise show an error
        if status_code == 200:
            # Extract and display only the label under predictions[0]
            if 'prediction' in json_response and len(json_response['prediction']) > 0:
                label = json_response['prediction']['label']
                probability = json_response['prediction']['probability']
                calories = json_response['prediction']['calories_per_100g']

                col1, col2 = st.columns(2)
                with col1:
                    st.image(uploaded_file, caption='Uploaded Image', use_container_width=True)
                with col2:
                    st.metric(label="Predicted Label", value=f"{label} 🏷️")
                    st.metric(label="Confidence", value=f"{probability*100:.2f}%")
                    st.metric(label="Calories", value=f"{calories:.2f} kcal")
                    st.write(f"It provides {calories:.2f} kilocalories of energy if you eat 100 grams of {label} ⚡")

                st.success("Prediction completed successfully!")
                if st.button("Upload Another Image"):
                # Clear the uploaded file from session state
                    if "uploaded_file" in st.session_state:
                        del st.session_state["uploaded_file"]
                    st.rerun()


            else:
                st.error("No predictions found in the API response.")
        else:
            # Show error message if status code is not 200
            st.error(f"Error: API returned status code {status_code}. Please check the server.")
