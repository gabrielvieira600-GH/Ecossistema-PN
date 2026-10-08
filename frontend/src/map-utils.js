export const bounds = (points) => {
  const xs = points.map((p) => p[0]),
    ys = points.map((p) => p[1]);
  return {
    x: Math.min(...xs),
    y: Math.min(...ys),
    w: Math.max(...xs) - Math.min(...xs),
    h: Math.max(...ys) - Math.min(...ys),
  };
};
export function transform(points, { x, y, w, h }) {
  const b = bounds(points);
  return points.map((p) => [
    +(x + ((p[0] - b.x) * w) / b.w).toFixed(3),
    +(y + ((p[1] - b.y) * h) / b.h).toFixed(3),
  ]);
}
export function expandGroup(booth, booths) {
  return booth.group_id
    ? booths.filter((b) => b.group_id === booth.group_id)
    : [booth];
}
export const selection = (booths, assembly = false) => ({
  ids: booths.map((b) => b.id),
  versions: Object.fromEntries(booths.map((b) => [b.id, b.version])),
  assembly,
});
export const money = (n) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    n ?? 0,
  );
export const date = (n) => (n ? new Date(n).toLocaleString("pt-BR") : "");
export const statusNames = {
  AVAILABLE: "Disponível",
  RESERVED: "Reservado",
  CONTRACTED: "Contratado",
  BLOCKED: "Ocupação existente",
};
