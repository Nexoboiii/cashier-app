import { useState } from 'react'
import Till from './components/Till'
import Products from './components/Products'

export default function App() {
  const [screen, setScreen] = useState('till')

  return (
    <>
      <nav className="nav">
        <button className={screen === 'till' ? 'on' : ''} onClick={() => setScreen('till')}>Till</button>
        <button className={screen === 'products' ? 'on' : ''} onClick={() => setScreen('products')}>Products</button>
      </nav>
      {screen === 'till' ? <Till /> : <Products />}
    </>
  )
}