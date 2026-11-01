import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Button, Card, Container, Spinner } from 'react-bootstrap';
import api from '../../services/api';
import { clearSession, getSession } from '../../services/session';
import { homeForRole, roleName } from '../../services/roles';

export default function Portal() {
  const [articles, setArticles] = useState(null);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [expanded, setExpanded] = useState(null);
  const [session, setSession] = useState(getSession);
  useEffect(() => {
    const controller = new AbortController();
    api.get('/portal/news', { signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) { setArticles(data); setError(''); } })
      .catch(() => { if (!controller.signal.aborted) setError('Unable to load campus news.'); });
    return () => controller.abort();
  }, [attempt]);
  return (
    <Container className="py-4">
      <div className="d-flex flex-wrap gap-3 justify-content-between align-items-center mb-4">
        <h1>Campus news</h1><div className="d-flex gap-3 align-items-center">
          {session ? <>
            <span>{session.user.accountName} · {roleName(session.user.accountRole)}</span>
            <Link to="/account">My account</Link>
            {session.user.accountRole !== 5 && <Link to={homeForRole(session.user.accountRole)}>Dashboard</Link>}
            <Button variant="outline-secondary" onClick={() => { clearSession(); setSession(null); }}>Sign out</Button>
          </> : <><Link to="/login">Sign in</Link><Link to="/register">Register</Link></>}
        </div>
      </div>
      {error && <Alert variant="danger">{error} <Button onClick={() => setAttempt((value) => value + 1)}>Retry</Button></Alert>}
      {!articles && !error && <Spinner animation="border" role="status" />}
      {articles?.length === 0 && <Alert variant="info">No published articles yet.</Alert>}
      {articles?.map((article) => <Card className="mb-3" key={article.newsArticleId}><Card.Body>
        <Card.Title>{article.newsTitle}</Card.Title>
        <Card.Subtitle className="mb-2 text-muted">{article.categoryName} · {article.createdByName}</Card.Subtitle>
        <Card.Text>{article.headline}</Card.Text>
        {expanded === article.newsArticleId && <p style={{ whiteSpace: 'pre-wrap' }}>{article.newsContent}</p>}
        <Button variant="link" className="p-0" onClick={() => setExpanded(expanded === article.newsArticleId ? null : article.newsArticleId)}>
          {expanded === article.newsArticleId ? 'Show less' : 'Read article'}
        </Button>
      </Card.Body></Card>)}
    </Container>
  );
}
