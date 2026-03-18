# AI Image Processor

Ứng dụng web xử lý ảnh gồm:

- Khử nhiễu ảnh bằng OpenCV (`fastNlMeansDenoisingColored`)
- Nhận diện đối tượng bằng YOLOv5 (OpenCV DNN)
- Phân tích ảnh bằng Gemini Vision (mô tả + bổ sung nhãn)

## Tech stack

- Backend: Spring Boot 3.2, Java 21, MySQL
- Frontend: React 18, Vite 5
- Xử lý ảnh: OpenCV Java + YOLOv5 ONNX

## Cấu trúc

```
my-ai-app/
├── backend-spring/
└── frontend-react/
```

## Chạy local

Yêu cầu:

- Java 21
- Maven 3.9+
- Node.js 18+
- MySQL 8+

1. Backend

- Copy `backend-spring/.env.example` thành `backend-spring/.env`
- Điền biến môi trường thật (`SPRING_DATASOURCE_*`, `GEMINI_API_KEY`)
- Đặt model YOLO tại `backend-spring/src/main/resources/models/yolov5s.onnx`
- Chạy:

```bash
cd backend-spring
mvn spring-boot:run
```

2. Frontend

- Copy `frontend-react/.env.example` thành `frontend-react/.env`
- Chạy:

```bash
cd frontend-react
npm install
npm run dev
```

## Deploy public (khuyến nghị)

- Backend: Render (root `backend-spring`)
- Frontend: Vercel (root `frontend-react`)

Biến môi trường chính:

- Backend: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GEMINI_API_KEY`, `APP_YOLO_ENABLED`, `APP_YOLO_MODEL_PATH`, `APP_CORS_ALLOWED_ORIGINS`
- Frontend: `VITE_API_BASE_URL`, `VITE_ASSET_BASE_URL`

## API chính

- `POST /api/images/upload`
- `POST /api/images/denoise?strength=10`
- `POST /api/images/detect`
- `GET /api/images/{id}`
- `GET /api/images/history`
- `GET /api/images/stats`
- `GET /api/images/health`
- `DELETE /api/images/{id}`
- `DELETE /api/images/history`

## Lưu ý bảo mật

- Không commit file `.env`
- Không hardcode API key trong source
- Nếu key từng bị lộ, hãy rotate key
