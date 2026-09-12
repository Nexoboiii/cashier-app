import { useEffect, useState } from 'react'

const money = (r) => 'Rs ' + r.toLocaleString('en-LK')

export default function Till() {
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState([])
  const [tendered, setTendered] = useState('')
  const [error, setError] = useState(null)
  const [lastSale, setLastSale] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => { load() }, [])

  async function load() {
    const res = await fetch('/api/products')
    if (!res.ok) return setError('could not load products')
    setProducts(await res.json())
  }

  const total = cart.reduce((s, l) => s + l.unitPrice * l.quantity, 0)
  const inCart = (id) => cart.find((l) => l.productId === id)?.quantity ?? 0
  const lowStock = products.filter((p) => p.stockQuantity <= p.lowStockThreshold && p.stockQuantity > 0)
  const soldOut = products.filter((p) => p.stockQuantity === 0)

  const tenderedNum = Number(tendered)
  const changeDue = tendered !== '' && tenderedNum >= total ? tenderedNum - total : null

  function add(p) {
    setLastSale(null)
    setError(null)
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
    setError(null)
  }

  async function pay(paymentMethod) {
    if (cart.length === 0 || busy) return
    setBusy(true)
    setError(null)

    const res = await fetch('/api/sales', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        lines: cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
        paymentMethod,
        cashTenderedMinorUnits: paymentMethod === 'CASH' ? tenderedNum : null,
      }),
    })

    setBusy(false)

    if (!res.ok) {
      const body = await res.json().catch(() => null)
      return setError(body?.error ?? `sale failed (${res.status})`)
    }

    setLastSale(await res.json())
    setCart([])
    setTendered('')
    load()
  }

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
        <button className="big" onClick={() => setLastSale(null)}>New sale</button>
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
        {products.map((p) => {
          const left = p.stockQuantity - inCart(p.id)
          return (
            <button key={p.id} className="tile" onClick={() => add(p)} disabled={left <= 0}>
              <span className="tile-name">{p.name}</span>
              <span className="tile-price">{money(p.priceMinorUnits)}</span>
              <span className="tile-stock">{left} left</span>
            </button>
          )
        })}
      </div>

      <div className="cart">
        <h2>Cart</h2>
        {error && <p className="error">{error}</p>}
        {cart.length === 0 && <p className="muted">Tap a product</p>}

        {cart.map((l) => (
          <div key={l.productId} className="cart-line">
            <span className="cart-name">{l.name}</span>
            <button onClick={() => bump(l.productId, -1)}>−</button>
            <span className="qty">{l.quantity}</span>
            <button onClick={() => bump(l.productId, +1)}>+</button>
            <span className="cart-total">{money(l.unitPrice * l.quantity)}</span>
            <button onClick={() => removeLine(l.productId)}>✕</button>
          </div>
        ))}

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
          <button className="big" disabled={busy || cart.length === 0 || changeDue === null}
                  onClick={() => pay('CASH')}>Cash</button>
          <button className="big" disabled={busy || cart.length === 0}
                  onClick={() => pay('CARD')}>Card</button>
        </div>

        <button onClick={clearCart} disabled={cart.length === 0}>Clear cart</button>
      </div>
    </div>
  )
}