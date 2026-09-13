import { Fragment, useEffect, useState } from 'react'

const money = (r) => 'Rs ' + r.toLocaleString('en-LK')
const when = (iso) => new Date(iso).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

export default function Report() {
  const [report, setReport] = useState(null)
  const [collapsed, setCollapsed] = useState(() => new Set())
  const [openingFloat, setOpeningFloat] = useState('')
  const [counted, setCounted] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => { loadCurrent() }, [])

  async function failureMessage(res) {
    const body = await res.json().catch(() => null)
    return body?.error ?? `request failed (${res.status})`
  }

  async function loadCurrent() {
    setError('')
    const res = await fetch('/api/days/current')
    if (res.status === 204) { setReport(null); return }
    if (!res.ok) return setError('could not load the day')
    const day = await res.json()
    loadReport(day.id)
  }

  async function loadReport(id) {
    setError('')
    const res = await fetch(`/api/days/${id}/report`)
    if (!res.ok) return setError('could not load the report')
    setReport(await res.json())
  }

  function toggle(supplier) {
    setCollapsed((prev) => {
      const next = new Set(prev)
      if (next.has(supplier)) next.delete(supplier)
      else next.add(supplier)
      return next
    })
  }

  async function openDay(e) {
    e.preventDefault()
    setBusy(true)
    try {
      const res = await fetch('/api/days/open', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ openingFloatMinorUnits: Number(openingFloat) }),
      })
      if (!res.ok) return setError(await failureMessage(res))
      setOpeningFloat('')
      loadCurrent()
    } catch {
      setError('could not reach the till')
    } finally {
      setBusy(false)
    }
  }

  async function closeDay(e) {
    e.preventDefault()
    if (!window.confirm('Close the day? A closed day cannot be reopened.')) return
    setBusy(true)
    try {
      const res = await fetch('/api/days/close', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ countedCashMinorUnits: Number(counted), note: note || null }),
      })
      if (!res.ok) return setError(await failureMessage(res))
      const day = await res.json()
      setCounted('')
      setNote('')
      loadReport(day.id)
    } catch {
      setError('could not reach the till')
    } finally {
      setBusy(false)
    }
  }

  if (!report) {
    return (
      <div className="page">
        <h2>No day open</h2>
        <p className="muted">Count the cash in the tin and enter it before you start trading.</p>
        <form className="row" onSubmit={openDay}>
          <input type="number" min="0" step="1" placeholder="Opening float"
                 value={openingFloat} onChange={(e) => setOpeningFloat(e.target.value)} required />
          <button className="btn-primary" disabled={busy || openingFloat === ''}>Open the day</button>
        </form>
        {error && <p className="error">{error}</p>}
      </div>
    )
  }

  const closed = report.closedAt != null
  const variance = report.varianceMinorUnits
  const supplierUnits = report.suppliers.reduce((s, x) => s + x.units, 0)
  const supplierRevenue = report.suppliers.reduce((s, x) => s + x.revenueMinorUnits, 0)

  return (
    <div className="page">
      <div className="row no-print">
        <button onClick={() => (closed ? loadReport(report.dayId) : loadCurrent())}>Refresh</button>
        <button className="btn-primary" onClick={() => window.print()}>Print</button>
        {closed && <button onClick={() => setReport(null)}>Open a new day</button>}
      </div>

      <h2>Day {report.dayId} — {closed ? 'closed' : 'trading'}</h2>
      <p className="muted">
        Opened {when(report.openedAt)}
        {report.closedAt && ` · closed ${when(report.closedAt)}`}
      </p>

      <div className="tiles">
        <div className="stat"><span>Takings</span><strong>{money(report.totalTakingsMinorUnits)}</strong></div>
        <div className="stat"><span>Cash</span><strong>{money(report.cashTakingsMinorUnits)}</strong></div>
        <div className="stat"><span>Card</span><strong>{money(report.cardTakingsMinorUnits)}</strong></div>
        <div className="stat"><span>Sales</span><strong>{report.saleCount}</strong></div>
        <div className="stat"><span>Average</span><strong>{money(report.averageSaleMinorUnits)}</strong></div>
      </div>

      <h3>By supplier</h3>
      {report.suppliers.length === 0 ? <p className="muted">Nothing yet.</p> : (
        <table className="grouped">
          <thead>
            <tr>
              <th>Supplier / item</th>
              <th className="num">Units</th>
              <th className="num">Revenue</th>
            </tr>
          </thead>
          <tbody>
            {report.suppliers.map((s) => {
              const shut = collapsed.has(s.supplier)
              return (
                <Fragment key={s.supplier}>
                  <tr className="group" onClick={() => toggle(s.supplier)}>
                    <td>
                      <span className="twisty">{shut ? '▸' : '▾'}</span>
                      {s.supplier}
                      <span className="sub">{s.units} {s.units === 1 ? 'unit' : 'units'}</span>
                    </td>
                    <td className="num">{s.units}</td>
                    <td className="num">{money(s.revenueMinorUnits)}</td>
                  </tr>
                  {s.items.map((i) => (
                    <tr className={shut ? 'item is-hidden' : 'item'} key={s.supplier + '|' + i.name}>
                      <td>{i.name}</td>
                      <td className="num">{i.units}</td>
                      <td className="num">{money(i.revenueMinorUnits)}</td>
                    </tr>
                  ))}
                </Fragment>
              )
            })}
            <tr className="total-row">
              <td>Total</td>
              <td className="num">{supplierUnits}</td>
              <td className="num">{money(supplierRevenue)}</td>
            </tr>
          </tbody>
        </table>
      )}

      <h3>Cash reconciliation</h3>
      <table className="recon">
        <tbody>
          <tr><td>Opening float</td><td>{money(report.openingFloatMinorUnits)}</td></tr>
          <tr><td>Cash taken in</td><td>+ {money(report.cashTenderedMinorUnits)}</td></tr>
          <tr><td>Change given out</td><td>− {money(report.changeGivenMinorUnits)}</td></tr>
          <tr className="expected"><td>Expected in the tin</td><td>{money(report.expectedCashMinorUnits)}</td></tr>
          {closed && (
            <>
              <tr><td>Counted</td><td>{money(report.countedCashMinorUnits)}</td></tr>
              <tr className={variance === 0 ? 'ok' : 'off'}>
                <td>Variance</td>
                <td>{variance === 0 ? money(0) : (variance > 0 ? '+ ' : '− ') + money(Math.abs(variance))}</td>
              </tr>
            </>
          )}
        </tbody>
      </table>

      {closed && report.closeNote && <p className="muted">Note: {report.closeNote}</p>}

      {!closed && (
        <form className="row no-print" onSubmit={closeDay}>
          <input type="number" min="0" step="1" placeholder="Counted in the tin"
                 value={counted} onChange={(e) => setCounted(e.target.value)} required />
          <input placeholder="Note (optional)" value={note} onChange={(e) => setNote(e.target.value)} />
          <button className="btn-primary" disabled={busy || counted === ''}>Close the day</button>
        </form>
      )}

      <h3>Exceptions</h3>
      <p className="muted">Everything today that was not a completed sale.</p>
      {report.exceptions.length === 0 ? <p className="muted">None.</p> : (
        <table>
          <thead><tr><th>Time</th><th>Event</th><th>Amount</th><th>Detail</th></tr></thead>
          <tbody>
            {report.exceptions.map((x, n) => (
              <tr key={n}>
                <td>{when(x.at)}</td>
                <td>{x.type}</td>
                <td>{x.amountMinorUnits === null ? '' : money(x.amountMinorUnits)}</td>
                <td>{x.detail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {error && <p className="error">{error}</p>}
    </div>
  )
}