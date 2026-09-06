import { useEffect, useState } from 'react'

const EMPTY = { name: '', priceMinorUnits: '', stockQuantity: '', lowStockThreshold: '' }

// whole rupees - no division, ever
const money = (r) => 'Rs ' + r.toLocaleString('en-LK')

export default function Products() {
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState(null)
  const [importResult, setImportResult] = useState(null)

  useEffect(() => { load() }, [])

  async function load() {
    const res = await fetch('/api/products')
    if (!res.ok) return setError('could not load products')
    setProducts(await res.json())
  }

  async function failureMessage(res) {
    const body = await res.json().catch(() => null)
    return body?.error ?? `request failed (${res.status})`
  }

  function edit(p) {
    setEditingId(p.id)
    setForm({
      name: p.name,
      priceMinorUnits: String(p.priceMinorUnits),
      stockQuantity: String(p.stockQuantity),
      lowStockThreshold: String(p.lowStockThreshold),
    })
    setError(null)
  }

  function cancel() {
    setEditingId(null)
    setForm(EMPTY)
    setError(null)
  }

  async function submit(e) {
    e.preventDefault()
    setError(null)
    const editing = editingId !== null

    const body = editing
      ? {
          name: form.name,
          priceMinorUnits: Number(form.priceMinorUnits),
          lowStockThreshold: Number(form.lowStockThreshold),
        }
      : {
          name: form.name,
          priceMinorUnits: Number(form.priceMinorUnits),
          stockQuantity: Number(form.stockQuantity),
          lowStockThreshold: Number(form.lowStockThreshold),
        }

    const res = await fetch(editing ? `/api/products/${editingId}` : '/api/products', {
      method: editing ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })

    if (!res.ok) return setError(await failureMessage(res))
    cancel()
    load()
  }

  async function importCsv(e) {
    const file = e.target.files?.[0]
    if (!file) return
    setError(null)
    setImportResult(null)

    const data = new FormData()
    data.append('file', file)

    const res = await fetch('/api/products/import', { method: 'POST', body: data })
    e.target.value = ''

    if (!res.ok) return setError(await failureMessage(res))
    setImportResult(await res.json())
    load()
  }

  const field = (k) => ({
    value: form[k],
    onChange: (e) => setForm({ ...form, [k]: e.target.value }),
  })

  return (
    <div className="page">
      <h1>Products</h1>

      {error && <p className="error">{error}</p>}

      <form onSubmit={submit} className="row">
        <input {...field('name')} placeholder="name" required />
        <input {...field('priceMinorUnits')} placeholder="price (rupees)" type="number" required />
        <input
          {...field('stockQuantity')}
          placeholder="stock"
          type="number"
          required={editingId === null}
          disabled={editingId !== null}
          title={editingId !== null ? 'stock changes through sales and restocks only' : ''}
        />
        <input {...field('lowStockThreshold')} placeholder="low stock at" type="number" required />
        <button type="submit">{editingId === null ? 'Add' : 'Save'}</button>
        {editingId !== null && <button type="button" onClick={cancel}>Cancel</button>}
      </form>

      <div className="row">
        <label className="button">
          Import CSV
          <input type="file" accept=".csv" onChange={importCsv} hidden />
        </label>
        <a className="button" href="/api/products/export">Export CSV</a>
      </div>

      {importResult && (
        <div className="summary">
          <strong>
            {importResult.created} created, {importResult.updated} updated, {importResult.skipped} skipped
          </strong>
          {importResult.errors.length > 0 && (
            <ul>
              {importResult.errors.map((e) => (
                <li key={e.line}>line {e.line}: {e.reason}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <table>
        <thead>
          <tr>
            <th>Name</th><th>Price</th><th>Stock</th><th>Low at</th><th></th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id} className={p.stockQuantity <= p.lowStockThreshold ? 'low' : ''}>
              <td>{p.name}</td>
              <td>{money(p.priceMinorUnits)}</td>
              <td>{p.stockQuantity}</td>
              <td>{p.lowStockThreshold}</td>
              <td><button onClick={() => edit(p)}>Edit</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}