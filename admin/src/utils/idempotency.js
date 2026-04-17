function stableStringify(value) {
  if (value === null || value === undefined) {
    return 'null'
  }
  if (Array.isArray(value)) {
    return '[' + value.map(item => stableStringify(item)).join(',') + ']'
  }
  if (typeof value === 'object') {
    return '{' + Object.keys(value).sort().map(key => `${key}:${stableStringify(value[key])}`).join(',') + '}'
  }
  return String(value)
}

function hashString(input) {
  let hash = 5381
  for (let i = 0; i < input.length; i++) {
    hash = ((hash << 5) + hash) + input.charCodeAt(i)
    hash = hash & 0xffffffff
  }
  return (hash >>> 0).toString(16)
}

export function buildCreateIdempotencyKey(scope, payload) {
  return `${scope}:${hashString(stableStringify(payload))}`
}
