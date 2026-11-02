import { Alert } from 'react-bootstrap';
import { useOutletContext } from 'react-router-dom';

export default function RequireAdmin({ children }) {
  const { user } = useOutletContext();
  return user.accountRole === 1
    ? children
    : <Alert variant="warning">Bạn không có quyền thực hiện chức năng này!</Alert>;
}
