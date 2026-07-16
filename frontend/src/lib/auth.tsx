import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import { api, setToken, clearToken, getToken } from './api'
import type { User } from './types'

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = getToken()
    if (token) {
      api.get<User>('/auth/me')
        .then((res) => setUser(res.data))
        .catch(() => clearToken())
        .finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [])

  async function login(username: string, password: string) {
    const res = await api.post<{ token: string; user: User }>('/auth/login', { username, password })
    setToken(res.data.token)
    setUser(res.data.user)
  }

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}