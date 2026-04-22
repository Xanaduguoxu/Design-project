export async function fetchDictionary(http, classify) {
  const res = await http.get('/v1/dictionary/list?classify=' + classify)
  const body = res && res.data
  const arr = Array.isArray(body && body.data) ? body.data : []
  const code2value = {}
  const value2code = {}
  arr.forEach(d => {
    if (d && d.code) code2value[d.code] = d.value
    if (d && d.value) value2code[d.value] = d.code
  })
  return { list: arr, map: { code2value, value2code }, code: body && body.code, message: body && body.message }
}