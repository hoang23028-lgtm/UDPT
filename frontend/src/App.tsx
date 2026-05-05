import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { QuizDetail } from './pages/QuizDetail';
import { Leaderboard } from './pages/Leaderboard';
import { CreateQuiz } from './pages/CreateQuiz';
import { Profile } from './pages/Profile';
import { ManageQuizzes } from './pages/ManageQuizzes';
import { EditQuiz } from './pages/EditQuiz';
import { AdminUsers } from './pages/AdminUsers';
import { MyResults } from './pages/MyResults';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/quiz/:id" element={<QuizDetail />} />
        <Route path="/quiz/:id/leaderboard" element={<Leaderboard />} />
        <Route path="/admin/create" element={<CreateQuiz />} />
        <Route path="/admin/quizzes" element={<ManageQuizzes />} />
        <Route path="/admin/quizzes/:id/edit" element={<EditQuiz />} />
        <Route path="/admin/users" element={<AdminUsers />} />
        <Route path="/me/results" element={<MyResults />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
