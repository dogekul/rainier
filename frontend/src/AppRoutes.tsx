import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import DemandsPage from './pages/Demand';
import DemandRequirementsPage from './pages/DemandRequirement';
import FeaturesPage from './pages/Feature';
import Workbench from './pages/Workbench';
import Login from './pages/Login';
import AuditLogsPage from './pages/AuditLog';
import OrganizationsPage from './pages/Organization';
import PositionsPage from './pages/Position';
import ProductModulesPage from './pages/ProductModule';
import ProductsPage from './pages/Product';
import ProjectsPage from './pages/Project';
import RequirementsPage from './pages/Requirement';
import SprintsPage from './pages/Sprint';
import TasksPage from './pages/Task';
import RolesPage from './pages/Role';
import UsersPage from './pages/User';
import UserOrganizationsPage from './pages/UserOrganization';
import UserRolesPage from './pages/UserRole';

/** App route tree. Exported separately from {@link App} so tests can wrap with MemoryRouter. */
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Workbench />} />
          <Route path="/org" element={<Navigate to="/org/organizations" replace />} />
          <Route path="/org/organizations" element={<OrganizationsPage />} />
          <Route path="/org/users" element={<UsersPage />} />
          <Route path="/org/user-organizations" element={<UserOrganizationsPage />} />
          <Route path="/pm" element={<Navigate to="/pm/projects" replace />} />
          <Route path="/pm/products" element={<ProductsPage />} />
          <Route path="/pm/product-modules" element={<ProductModulesPage />} />
          <Route path="/pm/features" element={<FeaturesPage />} />
          <Route path="/pm/projects" element={<ProjectsPage />} />
          <Route path="/pm/sprints" element={<SprintsPage />} />
          <Route path="/pm/tasks" element={<TasksPage />} />
          <Route path="/pm/demands" element={<DemandsPage />} />
          <Route path="/pm/requirements" element={<RequirementsPage />} />
          <Route path="/pm/demand-requirements" element={<DemandRequirementsPage />} />
          <Route path="/hr" element={<Navigate to="/hr/positions" replace />} />
          <Route path="/hr/positions" element={<PositionsPage />} />
          <Route path="/hr/roles" element={<RolesPage />} />
          <Route path="/hr/user-roles" element={<UserRolesPage />} />
          <Route path="/sys" element={<Navigate to="/sys/audit-logs" replace />} />
          <Route path="/sys/audit-logs" element={<AuditLogsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
