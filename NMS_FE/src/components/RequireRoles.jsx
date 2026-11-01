import { Alert } from 'react-bootstrap';
import { useOutletContext } from 'react-router-dom';
import { canAccess } from '../services/roles';

export default function RequireRoles({ roles, children }) {
  const { user } = useOutletContext();
  return canAccess(user, roles) ? children : <Alert variant="warning">Bạn không có quyền thực hiện chức năng này!</Alert>;
}
