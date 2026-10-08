import test from "node:test";
import assert from "node:assert/strict";
import { bounds, transform, expandGroup, selection } from "./map-utils.js";
test("moving and resizing preserves polygon topology", () => {
  const p = [
    [0, 0],
    [40, 0],
    [40, 20],
    [20, 20],
    [20, 40],
    [0, 40],
  ];
  const result = transform(p, { x: 50, y: 80, w: 80, h: 120 });
  assert.deepEqual(bounds(result), { x: 50, y: 80, w: 80, h: 120 });
  assert.equal(result.length, 6);
  assert.deepEqual(result[3], [90, 140]);
});
test("a grouped booth selects all group members and sends versions", () => {
  const a = { id: "a", version: 1, group_id: "group" },
    b = { id: "b", version: 3, group_id: "group" },
    c = { id: "c", version: 0 };
  const group = expandGroup(a, [a, b, c]);
  assert.deepEqual(group, [a, b]);
  assert.deepEqual(selection(group), {
    ids: ["a", "b"],
    versions: { a: 1, b: 3 },
    assembly: false,
  });
});
