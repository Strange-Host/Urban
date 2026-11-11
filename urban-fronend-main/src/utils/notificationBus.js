const bus = new EventTarget();

export function publishNotification(notification) {
  bus.dispatchEvent(new CustomEvent("notification", { detail: notification }));
}

export function onNotification(handler) {
  const wrapped = (e) => handler(e.detail);
  bus.addEventListener("notification", wrapped);
  return () => bus.removeEventListener("notification", wrapped);
}
// raw
export default { publishNotification, onNotification };
