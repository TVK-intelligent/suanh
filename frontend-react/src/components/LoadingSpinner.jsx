/**
 * Component Loading Spinner với overlay
 */
function LoadingSpinner({ message = 'Đang xử lý...', progress }) {
  return (
    <div className="spinner-overlay">
      <div className="spinner-container">
        <div className="spinner"></div>
        <p className="spinner-text">{message}</p>
        {progress !== undefined && progress > 0 && (
          <p className="spinner-text" style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>
            {progress}%
          </p>
        )}
      </div>
    </div>
  );
}

export default LoadingSpinner;
