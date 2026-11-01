# NMS Frontend

Login uses `POST /api/v1/auth/login` with the account email and password stored in SQL Server.
The application verifies the signed-in account through `GET /api/v1/auth/me` and stores the access/refresh token pair. On HTTP 401 it refreshes once through `POST /api/v1/auth/refresh` and retries; concurrent requests share a refresh. Invalid refresh ends the session, while HTTP 403 displays a permission message and keeps the session.

Start the backend on port 8080, then run `npm install` and `npm run dev` here.
Vite forwards `/api` requests to `http://localhost:8080`; restart Vite after changing its configuration.
For a different backend, set `VITE_API_BASE_URL` (including `/api/v1`) in `.env.local` before starting/building.
Production hosting must proxy `/api` to the backend or provide that environment variable with the appropriate backend CORS configuration.

User Management uses the authenticated `/api/v1/users` API for list/create/update/delete with no mock users or local fallback.
Use Apply filters (or Enter) to search by keyword, ID, name, email and role; Reset clears the filters.
Pagination, page sizes (5/10/20/50/100), and sorting by ID/name/email/role run on the backend using Spring Pageable (`page`, `size`, `sort`).
An empty password on Edit keeps the current password. Validation and API errors stay in the form so failed changes can be corrected.
The dashboard reads real counts from `/api/v1/dashboard/overview`. Category and News management still use their existing mock data.

## React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

Security checks: `npm test`, `npm run lint`, `npm run build`.
