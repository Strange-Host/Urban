import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { FiBell } from "react-icons/fi";
import { api } from "../api/api";

function formatDateTime(dateString) {
  if (!dateString) return "";
  const date = new Date(dateString);
  return `${date.toLocaleDateString("en-CA")} ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
}

export default function NotificationBell() {
  const navigate = useNavigate();
  const [unread, setUnread] = useState([]);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef(null);

  const fetchUnread = useCallback(async () => {
    try {
      const res = await api.get("/api/v1/notifications/unread");
      setUnread(res.data);
    } catch (err) {
      console.error("Failed to fetch notifications", err);
    }
  }, []);

  useEffect(() => {
    fetchUnread();

    const token = localStorage.getItem("accessToken");
    if (!token) return;

    const base = api.defaults.baseURL || "";
    const es = new EventSource(
      `${base}/api/v1/notifications/subscribe?access_token=${encodeURIComponent(token)}`
    );

    es.addEventListener("notification", (e) => {
      try {
        const notification = JSON.parse(e.data);
        setUnread((prev) =>
          prev.some((n) => n.id === notification.id) ? prev : [notification, ...prev]
        );
      } catch (err) {
        console.error("Failed to parse notification", err);
      }
    });

    // Reload on (re)connect: events sent while the stream was down are lost.
    es.addEventListener("open", fetchUnread);

    return () => es.close();
  }, [fetchUnread]);

  // Fallback for when the stream cannot be established at all (proxy, offline).
  useEffect(() => {
    const intervalId = setInterval(fetchUnread, 300000);
    return () => clearInterval(intervalId);
  }, [fetchUnread]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const markAsRead = async (id) => {
    try {
      await api.patch(`/api/v1/notifications/${id}/read`);
      setUnread((prev) => prev.filter((n) => n.id !== id));
    } catch (err) {
      console.error("Failed to mark as read", err);
    }
  };

  const markAllRead = async () => {
    try {
      await api.patch("/api/v1/notifications/read-all");
      setUnread([]);
    } catch (err) {
      console.error("Failed to mark all as read", err);
    }
  };

  const handleClick = (notification) => {
    markAsRead(notification.id);
    setOpen(false);
    if (notification.referenceId) {
      navigate(`/crm/leads/${notification.referenceId}`);
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setOpen(!open)}
        aria-label="Notifications"
        className="relative grid h-[42px] w-[42px] place-items-center rounded-[10px] border border-slate-200 bg-white text-slate-600 shadow-sm hover:bg-slate-50"
      >
        <FiBell size={18} />
        {unread.length > 0 && (
          <span className="absolute -top-1.5 -right-1.5 bg-rose-600 text-white text-[10px] rounded-full min-w-5 h-5 px-1 flex items-center justify-center font-semibold ring-2 ring-white">
            {unread.length > 9 ? "9+" : unread.length}
          </span>
        )}
      </button>

      {open && (
        <div className="fixed inset-x-3 top-20 sm:absolute sm:inset-x-auto sm:top-full sm:right-0 sm:mt-2 sm:w-80 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 max-h-96 overflow-y-auto animate-pop-in">
          <div className="sticky top-0 bg-white flex items-center justify-between px-4 py-3 border-b border-slate-100">
            <span className="font-semibold text-sm">Notifications</span>
            {unread.length > 0 && (
              <button
                onClick={markAllRead}
                className="text-xs font-medium text-brand-600 hover:text-brand-700"
              >
                Mark all read
              </button>
            )}
          </div>

          {unread.length === 0 ? (
            <div className="px-4 py-6 text-sm text-slate-400 text-center">
              No unread notifications
            </div>
          ) : (
            unread.map((n) => (
              <div
                key={n.id}
                onClick={() => handleClick(n)}
                className="px-4 py-3 border-b border-slate-100 last:border-b-0 hover:bg-slate-50 cursor-pointer flex items-start gap-3"
              >
                <div className="mt-1.5 w-2 h-2 rounded-full bg-brand-500 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-slate-800 truncate">{n.title}</div>
                  <div className="text-xs text-slate-500 mt-0.5 truncate">{n.message}</div>
                  <div className="text-xs text-slate-400 mt-1">
                    {formatDateTime(n.sentAt)}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
