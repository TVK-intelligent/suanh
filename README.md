# AI Image Processor 🖼️✨

Ứng dụng xử lý ảnh AI với khả năng **Khử nhiễu (Super Resolution)** và **Nhận diện đối tượng (Object Detection)**.

## 🚀 Tech Stack

| Layer          | Technology                |
| -------------- | ------------------------- |
| **Backend**    | Spring Boot 3.2 + Java 21 |
| **AI Runtime** | ONNX Runtime 1.16.3       |
| **Database**   | MySQL 8.0                 |
| **Frontend**   | React 18 + Vite 5         |

## 📁 Cấu trúc dự án

```
my-ai-app/
├── backend-spring/     # Spring Boot API (Port 8080)
├── frontend-react/     # React UI (Port 5173)
└── uploads/            # Thư mục lưu ảnh
```

## ⚡ Hướng dẫn cài đặt

### 1. Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- MySQL 8.0

### 2. Tải ONNX Models

**Tải models và đặt vào thư mục:**

```
backend-spring/src/main/resources/models/
```

| Model          | Link                                                                                         | Mục đích                     |
| -------------- | -------------------------------------------------------------------------------------------- | ---------------------------- |
| Real-ESRGAN x4 | [Hugging Face](https://huggingface.co/Xenova/realesrgan-x4plus/resolve/main/onnx/model.onnx) | Khử nhiễu & Super Resolution |
| YOLOv5s        | [GitHub](https://github.com/ultralytics/yolov5/releases/download/v7.0/yolov5s.onnx)          | Nhận diện đối tượng          |

**Đổi tên files:**

- `model.onnx` → `realesrgan-x4plus.onnx`
- `yolov5s.onnx` → giữ nguyên

### 3. Cấu hình MySQL

```sql
CREATE DATABASE ai_image_db;
```

Cập nhật password trong `application.properties` nếu cần:

```properties
spring.datasource.password=12345678
```

### 4. Chạy Backend

```bash
cd backend-spring
mvn spring-boot:run
```

Backend sẽ chạy tại: http://localhost:8080

### 5. Chạy Frontend

```bash
cd frontend-react
npm install
npm run dev
```

Frontend sẽ chạy tại: http://localhost:5173

## 🌍 Deploy để ai cũng truy cập được

### Mô hình khuyến nghị

- **Backend (Spring Boot):** Render Web Service
- **Frontend (React):** Vercel
- **Database:** MySQL cloud (Aiven/PlanetScale/Railway MySQL)

### 1) Deploy Backend lên Render

1. Vào Render → **New +** → **Web Service** → chọn repo GitHub.
2. Root directory: `backend-spring`
3. Build command: `mvn clean package -DskipTests`
4. Start command: `java -jar target/*.jar`
5. Thêm Environment Variables:
	- `PORT=10000` (hoặc để Render tự set)
	- `SPRING_DATASOURCE_URL=jdbc:mysql://...`
	- `SPRING_DATASOURCE_USERNAME=...`
	- `SPRING_DATASOURCE_PASSWORD=...`
	- `GEMINI_API_KEY=...`
	- `APP_CORS_ALLOWED_ORIGINS=https://<ten-frontend>.vercel.app`

Sau khi deploy xong, bạn sẽ có URL backend dạng:

`https://your-backend.onrender.com`

### 2) Deploy Frontend lên Vercel

1. Vào Vercel → **Add New Project** → chọn repo GitHub.
2. Root directory: `frontend-react`
3. Framework preset: `Vite`
4. Thêm Environment Variables:
	- `VITE_API_BASE_URL=https://your-backend.onrender.com/api/images`
	- `VITE_ASSET_BASE_URL=https://your-backend.onrender.com`
5. Deploy.

Sau khi xong, mọi người có thể truy cập qua URL Vercel.

### 3) Cập nhật CORS backend

Khi đã có domain frontend chính thức, cập nhật lại:

- `APP_CORS_ALLOWED_ORIGINS=https://<ten-frontend>.vercel.app`

Rồi redeploy backend.

### 4) Biến môi trường mẫu

- Backend: `backend-spring/.env.example`
- Frontend: `frontend-react/.env.example`

## 🔐 Security Note

Bạn đã từng commit API key vào mã nguồn. Hãy **rotate (đổi) GEMINI_API_KEY ngay** trên Google AI Studio và chỉ dùng key qua biến môi trường.

## 🎯 API Endpoints

| Method   | Endpoint                          | Mô tả                                       |
| -------- | --------------------------------- | ------------------------------------------- |
| `POST`   | `/api/images/upload`              | Upload và xử lý ảnh (khử nhiễu + nhận diện) |
| `POST`   | `/api/images/denoise?strength=10` | Khử nhiễu ảnh (tuỳ chỉnh cường độ 1-30)     |
| `POST`   | `/api/images/detect`              | Nhận diện đối tượng bằng Gemini Vision      |
| `GET`    | `/api/images/{id}`                | Lấy thông tin ảnh                           |
| `GET`    | `/api/images/history?keyword=abc` | Lịch sử xử lý (phân trang + tìm kiếm)       |
| `GET`    | `/api/images/stats`               | Thống kê tổng quan                          |
| `GET`    | `/api/images/health`              | Health check                                |
| `DELETE` | `/api/images/{id}`                | Xóa một ảnh                                 |
| `DELETE` | `/api/images/history`             | Xóa toàn bộ lịch sử                         |

## ✨ Tính năng

- **3 chế độ xử lý**: Khử nhiễu, Nhận diện, Toàn diện (kết hợp cả hai)
- **Tuỳ chỉnh cường độ khử nhiễu**: Slider 1-30
- **Nhận diện đối tượng AI**: Gemini Vision API (phân tích + mô tả bằng tiếng Việt)
- **So sánh ảnh trước/sau**: Slider kéo qua lại
- **Đo lường chất lượng**: PSNR & SSIM
- **Text-to-Speech**: Đọc mô tả ảnh bằng giọng nói
- **Phóng to ảnh**: Click để xem fullscreen
- **Thống kê Dashboard**: Tổng ảnh, tỷ lệ thành công, thời gian TB
- **Tìm kiếm lịch sử**: Theo tên file
- **Toast notifications**: Thông báo đẹp mắt
- **Dark theme premium**: Glassmorphism + Neon accents

## 📸 Demo Mode

Nếu không có file model ONNX, ứng dụng sẽ chạy ở chế độ **Demo**:

- Khử nhiễu: Trả về ảnh gốc
- Nhận diện: Trả về object demo

## 🔧 Troubleshooting

**Lỗi CORS:**

- Đảm bảo frontend chạy ở port 5173

**Lỗi MySQL connection:**

- Kiểm tra MySQL đang chạy
- Kiểm tra username/password

**Model không tải được:**

- Kiểm tra đường dẫn file trong `application.properties`
- Kiểm tra quyền đọc file

## 📝 License

MIT License
