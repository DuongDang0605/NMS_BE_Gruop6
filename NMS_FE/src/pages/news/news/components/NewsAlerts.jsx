import { Alert, Button } from 'react-bootstrap';

const NewsAlerts = ({ successToast, onCloseSuccess, error, onRetry, actionError, onCloseAction }) => (
  <>
    {successToast && (
      <Alert
        variant="success"
        dismissible
        onClose={onCloseSuccess}
        className="d-flex align-items-center gap-2 shadow-sm rounded-3 py-2"
      >
        <i className="bi bi-check-circle-fill fs-5"></i>
        <span>{successToast}</span>
      </Alert>
    )}

    {error && (
      <Alert variant="danger" className="d-flex justify-content-between align-items-center rounded-3">
        <div className="d-flex align-items-center gap-2">
          <i className="bi bi-exclamation-triangle-fill fs-5"></i>
          <span>{error}</span>
        </div>
        <Button variant="outline-danger" size="sm" onClick={onRetry}>
          Thử lại
        </Button>
      </Alert>
    )}

    {actionError && (
      <Alert variant="danger" dismissible onClose={onCloseAction} className="rounded-3">
        {actionError}
      </Alert>
    )}
  </>
);

export default NewsAlerts;
