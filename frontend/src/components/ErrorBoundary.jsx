import { Component } from 'react'

export default class ErrorBoundary extends Component {
  state = { error: null }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('render crash', error, info)
  }

  render() {
    if (!this.state.error) return this.props.children

    return (
      <div className="page done">
        <p className="label">Something went wrong</p>
        <p className="muted">
          The screen crashed. Your sales are safe — nothing is lost by reloading.
        </p>
        <div className="row">
          <button className="big btn-primary" onClick={() => window.location.reload()}>Reload</button>
        </div>
        <p className="hint">{String(this.state.error?.message ?? this.state.error)}</p>
      </div>
    )
  }
}