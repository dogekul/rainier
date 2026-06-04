import '@testing-library/jest-dom/vitest';

/**
 * Polyfill Web Storage for jsdom 25 on Node 25.
 *
 * <p>In our toolchain, jsdom's {@code window.localStorage} returns a bare {@code Object} without
 * the Storage prototype methods (getItem/setItem/removeItem/clear). We install an in-memory
 * implementation per-test before any production code touches it.
 */
class MemoryStorage implements Storage {
  private store: Record<string, string> = {};

  get length(): number {
    return Object.keys(this.store).length;
  }

  clear(): void {
    this.store = {};
  }

  getItem(key: string): string | null {
    return Object.prototype.hasOwnProperty.call(this.store, key) ? this.store[key] : null;
  }

  key(index: number): string | null {
    return Object.keys(this.store)[index] ?? null;
  }

  removeItem(key: string): void {
    delete this.store[key];
  }

  setItem(key: string, value: string): void {
    this.store[key] = String(value);
  }
}

function isMissingStorageMethods(storage: unknown): boolean {
  return (
    !storage ||
    typeof (storage as Storage).getItem !== 'function' ||
    typeof (storage as Storage).setItem !== 'function' ||
    typeof (storage as Storage).removeItem !== 'function'
  );
}

if (typeof window !== 'undefined' && isMissingStorageMethods(window.localStorage)) {
  Object.defineProperty(window, 'localStorage', {
    value: new MemoryStorage(),
    configurable: true,
    writable: true,
  });
}

if (typeof window !== 'undefined' && isMissingStorageMethods(window.sessionStorage)) {
  Object.defineProperty(window, 'sessionStorage', {
    value: new MemoryStorage(),
    configurable: true,
    writable: true,
  });
}
