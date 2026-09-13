import { useEffect, useState } from 'react'

const money = (r) => 'Rs ' + r.toLocaleString('en-LK')

// 1-9 then 0 for the tenth tile
const hotkey = (i) => (i < 9 ? String(i + 1) : i === 9 ? '0' : null)

export default function Till() {
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState([])
  const [tendered, setTendered] = useState('')
  const [finder, setFinder] = useState('')
  const [error, setError] = useState(null)
  const [lastSale, setLastSale] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => { load() }, [])

  async function load() {
    const res = await fetch('/api/products')
    if (!res.ok) return setError('could not load products')
    setProducts(await res.json())
  }

  const visible = finder === ''
    ? products
    : products.filter((p) => p.name.toLowerCase().includes(finder.toLowerCase()))

  const total = cart.reduce((s, l) => s + l.unitPrice * l.quantity, 0)
  const itemCount = cart.reduce((s, l) => s + l.quantity, 0)
  const inCart = (id) => cart.find((l) => l.productId === id)?.quantity ?? 0
  const lowStock = products.filter((p) => p.stockQuantity <= p.lowStockThreshold && p.stockQuantity > 0)
  const soldOut = products.filter((p) => p.stockQuantity === 0)

  const tenderedNum = Number(tendered)
  const changeDue = tendered !== '' && tenderedNum >= total ? tenderedNum - total : null

  function add(p) {
    if (p.stockQuantity - inCart(p.id) <= 0) return
    setLastSale(null)
    setError(null)
    setFinder('')
    setCart((c) => {
      const line = c.find((l) => l.productId === p.id)
      if (line) return c.map((l) => (l.productId === p.id ? { ...l, quantity: l.quantity + 1 } : l))
      return [...c, { productId: p.id, name: p.name, unitPrice: p.priceMinorUnits, quantity: 1 }]
    })
  }

  function bump(productId, by) {
    const product = products.find((p) => p.id === productId)
    setCart((c) =>
      c.map((l) => {
        if (l.productId !== productId) return l
        const next = l.quantity + by
        if (product && next > product.stockQuantity) return l
        return { ...l, quantity: next }
      }).filter((l) => l.quantity > 0)
    )
  }

  function removeLine(productId) {
    setCart((c) => c.filter((l) => l.productId !== productId))
  }

  function clearCart() {
    setCart([])
    setTendered('')
    setFinder('')
    setError(null)
  }

  async function pay(paymentMethod) {
    if (cart.length === 0 || busy) return
    if (paymentMethod === 'CASH' && changeDue === null) return
    setBusy(true)
    setError(null)

    try {
      const res = await fetch('/api/sales', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          lines: cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
          paymentMethod,
          cashTenderedMinorUnits: paymentMethod === 'CASH' ? tenderedNum : null,
        }),
      })

      if (!res.ok) {
        const body = await res.json().catch(() => null)
        return setError(body?.error ?? `sale failed (${res.status})`)
      }

      setLastSale(await res.json())
      setCart([])
      setTendered('')
      setFinder('')
      load()
    } catch {
      setError('could not reach the till')
    } finally {
      setBusy(false)
    }
  }

  // hands stay on the keyboard - the queue is the reason this exists
  useEffect(() => {
    function onKey(e) {
      if (e.ctrlKey || e.altKey || e.metaKey) return
      const typing = /^(INPUT|SELECT|TEXTAREA)$/.test(document.activeElement?.tagName ?? '')

      if (lastSale) {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setLastSale(null) }
        return
      }

      if (e.key === 'Enter') {
        e.preventDefault()
        pay(e.shiftKey ? 'CARD' : 'CASH')
        return
      }

      if (e.key === 'F2') {
        e.preventDefault()
        if (total > 0) setTendered(String(total))
        return
      }

      // esc backs out one layer at a time
      if (e.key === 'Escape') {
        e.preventDefault()
        if (finder !== '') setFinder('')
        else clearCart()
        return
      }

      // everything below would fight the tender field
      if (typing) return

      if (/^[0-9]$/.test(e.key)) {
        const i = e.key === '0' ? 9 : Number(e.key) - 1
        if (visible[i]) { e.preventDefault(); add(visible[i]) }
        return
      }

      // backspace edits the search first, the cart second
      if (e.key === 'Backspace') {
        e.preventDefault()
        if (finder !== '') return setFinder((f) => f.slice(0, -1))
        const last = cart[cart.length - 1]
        if (last) bump(last.productId, -1)
        return
      }

      if (/^[a-zA-Z0-9 ]$/.test(e.key)) {
        e.preventDefault()
        setFinder((f) => f + e.key)
      }
    }

    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  if (lastSale) {
    return (
      <div className="page done">
        {lastSale.changeGivenMinorUnits !== null ? (
          <>
            <p className="label">Change</p>
            <p className="change-huge">{money(lastSale.changeGivenMinorUnits)}</p>
            <p className="muted">
              Total {money(lastSale.totalMinorUnits)} · cash {money(lastSale.cashTenderedMinorUnits)}
            </p>
          </>
        ) : (
          <>
            <p className="label">Card sale</p>
            <p className="change-huge">{money(lastSale.totalMinorUnits)}</p>
          </>
        )}
        <div className="row">
          <button className="big btn-primary" onClick={() => setLastSale(null)}>New sale</button>
        </div>
        <p className="hint"><kbd>Enter</kbd> for the next customer</p>
      </div>
    )
  }

  return (
    <div className="till">
      {(lowStock.length > 0 || soldOut.length > 0) && (
        <div className="stock-warn">
          {soldOut.length > 0 && <span className="out">{soldOut.length} sold out</span>}
          {lowStock.length > 0 && <span className="low">{lowStock.length} low</span>}
          <span className="muted">
            {[...soldOut, ...lowStock].map((p) => p.name).join(' · ')}
          </span>
        </div>
      )}

      <div className="grid">
        {finder !== '' && (
          <div className="finder">
            <span className="finder-label">Find</span>
            <span className="finder-text">{finder}</span>
            <span className="finder-count">
              {visible.length} {visible.length === 1 ? 'match' : 'matches'}
            </span>
            <button className="btn-ghost" onClick={() => setFinder('')}>✕</button>
          </div>
        )}

        {visible.length === 0 && <p className="muted">Nothing matches “{finder}”</p>}

        {visible.map((p, i) => {
          const left = p.stockQuantity - inCart(p.id)
          const key = hotkey(i)
          return (
            <button key={p.id} className="tile" onClick={() => add(p)} disabled={left <= 0}>
              {key && <span className="tile-key">{key}</span>}
              <span className="tile-name">{p.name}</span>
              <span className="tile-price">{money(p.priceMinorUnits)}</span>
              <span className="tile-stock">{left} left</span>
            </button>
          )
        })}
      </div>

      <div className="cart">
        <div className="cart-head">
          <h2>Cart</h2>
          <span className="cart-count">{itemCount} {itemCount === 1 ? 'item' : 'items'}</span>
        </div>

        {error && <p className="error">{error}</p>}

        {cart.length === 0 ? (
          <p className="cart-empty">Type to find, or press a number key</p>
        ) : (
          cart.map((l) => (
            <div key={l.productId} className="cart-line">
              <span className="cart-name">{l.name}</span>
              <button className="btn-ghost" onClick={() => bump(l.productId, -1)}>−</button>
              <span className="qty">{l.quantity}</span>
              <button className="btn-ghost" onClick={() => bump(l.productId, +1)}>+</button>
              <span className="cart-total">{money(l.unitPrice * l.quantity)}</span>
              <button className="btn-ghost" onClick={() => removeLine(l.productId)}>✕</button>
            </div>
          ))
        )}

        <p className="total-big">{money(total)}</p>

        <input
          className="tender"
          type="number"
          placeholder="cash received"
          value={tendered}
          onChange={(e) => setTendered(e.target.value)}
        />
        <div className="quick">
          <button onClick={() => setTendered(String(total))} disabled={total === 0}>Exact</button>
          {[100, 500, 1000, 2000, 5000, 10000].map((n) => (
            <button key={n} onClick={() => setTendered(String(n))} disabled={n < total}>
              {n.toLocaleString('en-LK')}
            </button>
          ))}
        </div>
        {changeDue !== null && <p className="change-preview">Change {money(changeDue)}</p>}

        <div className="pay">
          <button className="big btn-money" disabled={busy || cart.length === 0 || changeDue === null}
                  onClick={() => pay('CASH')}>Cash</button>
          <button className="big" disabled={busy || cart.length === 0}
                  onClick={() => pay('CARD')}>Card</button>
        </div>

        <button className="btn-ghost" onClick={clearCart} disabled={cart.length === 0}>Clear cart</button>

        <p className="hint">
          type to find · <kbd>1</kbd>–<kbd>0</kbd> add · <kbd>F2</kbd> exact ·{' '}
          <kbd>⌫</kbd> back · <kbd>Enter</kbd> cash · <kbd>⇧Enter</kbd> card · <kbd>Esc</kbd> clear
        </p>
      </div>
    </div>
  )
}