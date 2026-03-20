/**
 * Component hiển thị các tags nhận diện đối tượng
 */
function DetectionTags({
  tags,
  yoloLabels,
  processingTime,
  imageWidth,
  imageHeight,
}) {
  if (!tags || tags.length === 0) {
    return null;
  }

  // Emoji mapping cho các đối tượng phổ biến
  const getEmoji = (label) => {
    const emojiMap = {
      person: "👤",
      bicycle: "🚲",
      car: "🚗",
      motorcycle: "🏍️",
      airplane: "✈️",
      bus: "🚌",
      train: "🚂",
      truck: "🚛",
      boat: "⛵",
      bird: "🐦",
      cat: "🐱",
      dog: "🐕",
      horse: "🐴",
      sheep: "🐑",
      cow: "🐄",
      elephant: "🐘",
      bear: "🐻",
      zebra: "🦓",
      giraffe: "🦒",
      backpack: "🎒",
      umbrella: "☂️",
      handbag: "👜",
      tie: "👔",
      suitcase: "🧳",
      bottle: "🍼",
      cup: "☕",
      fork: "🍴",
      knife: "🔪",
      spoon: "🥄",
      bowl: "🥣",
      banana: "🍌",
      apple: "🍎",
      sandwich: "🥪",
      orange: "🍊",
      broccoli: "🥦",
      carrot: "🥕",
      pizza: "🍕",
      donut: "🍩",
      cake: "🎂",
      chair: "🪑",
      couch: "🛋️",
      bed: "🛏️",
      toilet: "🚽",
      tv: "📺",
      laptop: "💻",
      mouse: "🖱️",
      keyboard: "⌨️",
      "cell phone": "📱",
      microwave: "📦",
      oven: "🔥",
      toaster: "🍞",
      refrigerator: "🧊",
      book: "📚",
      clock: "⏰",
      vase: "🏺",
      scissors: "✂️",
      "teddy bear": "🧸",
      toothbrush: "🪥",
      demo_object: "🎯",
    };

    return emojiMap[label.toLowerCase()] || "🏷️";
  };

  return (
    <div className="card fade-in mt-xl">
      <h3 className="card-title">
        <span>🤖</span>
        Kết quả nhận diện AI
      </h3>

      {/* Stats */}
      <div className="stats-grid">
        <div className="stat-item">
          <div className="stat-value">{tags.length}</div>
          <div className="stat-label">Đối tượng phát hiện</div>
        </div>
        {yoloLabels && yoloLabels.length > 0 && (
          <div className="stat-item">
            <div className="stat-value">{yoloLabels.length}</div>
            <div className="stat-label">YOLO phát hiện</div>
          </div>
        )}
        {processingTime && (
          <div className="stat-item">
            <div className="stat-value">{processingTime}ms</div>
            <div className="stat-label">Thời gian xử lý</div>
          </div>
        )}
        {imageWidth && imageHeight && (
          <div className="stat-item">
            <div className="stat-value">
              {imageWidth}×{imageHeight}
            </div>
            <div className="stat-label">Kích thước ảnh</div>
          </div>
        )}
      </div>

      {/* YOLO Tags */}
      {yoloLabels && yoloLabels.length > 0 && (
        <>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "0.75rem",
              fontSize: "0.95rem",
              opacity: 0.8,
            }}
          >
            🔍 Phát hiện bằng YOLO:
          </h4>
          <div className="tags-container">
            {yoloLabels.map((tag, index) => (
              <span
                key={`yolo-${index}`}
                className="tag"
                style={{
                  background: "rgba(59, 130, 246, 0.2)",
                  borderColor: "#3b82f6",
                }}
              >
                <span className="tag-icon">{getEmoji(tag)}</span>
                {tag}
              </span>
            ))}
          </div>
        </>
      )}

      {/* All Tags */}
      <h4
        style={{
          marginTop: "1.5rem",
          marginBottom: "0.75rem",
          fontSize: "0.95rem",
          opacity: 0.8,
        }}
      >
        📋 Tất cả phát hiện (YOLO + Gemini):
      </h4>
      <div className="tags-container">
        {tags.map((tag, index) => (
          <span key={index} className="tag">
            <span className="tag-icon">{getEmoji(tag)}</span>
            {tag}
          </span>
        ))}
      </div>
    </div>
  );
}

export default DetectionTags;
