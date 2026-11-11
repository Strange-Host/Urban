const navigate = useNavigate();

const handleLogout = async () => {
  try {
    await api.post("/api/v1/auth/logout");
  } catch (e) {
    console.log("Logout request failed, continuing anyway");
  }

  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");

  navigate("/login");
};