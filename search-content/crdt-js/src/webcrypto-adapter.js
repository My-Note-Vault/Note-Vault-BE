import { toUint8Array } from "js-base64";

const integerArrays = new Set([
  Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array,
  Int32Array, Uint32Array, BigInt64Array, BigUint64Array,
]);

export function getRandomValues(array) {
  if (!integerArrays.has(array?.constructor) || array.byteLength > 65536) {
    throw new TypeError("Invalid random array");
  }
  const bytes = new Uint8Array(array.buffer, array.byteOffset, array.byteLength);
  bytes.set(toUint8Array(globalThis.__yjsHost.randomBase64(bytes.length)));
  return array;
}

// Only getRandomValues is needed by this synchronous Yjs projection runtime.
export const subtle = undefined;
