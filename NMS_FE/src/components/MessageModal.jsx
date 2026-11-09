import { Modal, Button } from 'react-bootstrap';

function MessageModal({ show, title, message, onClose }) {
    return (
        <Modal show={show} onHide={onClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>{title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>{message}</Modal.Body>
            <Modal.Footer>
                <Button onClick={onClose}>OK</Button>
            </Modal.Footer>
        </Modal>
    );
}

export default MessageModal;