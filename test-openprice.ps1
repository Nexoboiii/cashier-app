# scratch - open-price smoke test. delete when done.
$base = 'http://localhost:8080/api'

function Invoke-Api {
  param([string]$Method = 'Get', [Parameter(Mandatory)][string]$Uri, $Body)
  try {
    $p = @{ Method = $Method; Uri = $Uri; ContentType = 'application/json' }
    if ($Body) { $p.Body = $Body }
    Invoke-RestMethod @p
  } catch {
    $r = $_.Exception.Response
    if ($r) {
      $sr = New-Object IO.StreamReader($r.GetResponseStream())
      Write-Host ("  HTTP {0} - {1}" -f [int]$r.StatusCode, $sr.ReadToEnd()) -ForegroundColor Red
    } else {
      Write-Host ("  " + $_.Exception.Message) -ForegroundColor Red
    }
  }
}

try { Invoke-RestMethod "$base/products" | Out-Null }
catch { Write-Host "The till is not running. Start it first." -ForegroundColor Yellow; exit 1 }

Write-Host "`n== open a day (fine if it says one is already open) ==" -ForegroundColor Cyan
Invoke-Api Post "$base/days/open" (@{openingFloatMinorUnits=5000} | ConvertTo-Json) | Out-Null

Write-Host "`n== create XO ==" -ForegroundColor Cyan
$xo = Invoke-Api Post "$base/products" (@{ name='XO'; priceMinorUnits=0; stockQuantity=9999;
      lowStockThreshold=0; supplier=$null; openPrice=$true } | ConvertTo-Json)
if (-not $xo) { $xo = (Invoke-Api Get "$base/products") | Where-Object { $_.name -eq 'XO' } }
$normal = (Invoke-Api Get "$base/products") | Where-Object { -not $_.openPrice } | Select-Object -First 1
Write-Host ("  XO id {0}; ordinary product = {1}" -f $xo.id, $normal.name)

Write-Host "`n== TEST 1: a price on an ORDINARY product must be REFUSED ==" -ForegroundColor Cyan
$r1 = Invoke-Api Post "$base/sales" (@{ lines=@(
    @{productId=$normal.id; quantity=1; unitPriceMinorUnits=1},
    @{productId=$normal.id; quantity=1}); paymentMethod='CARD' } | ConvertTo-Json -Depth 5)
if ($r1) { Write-Host "  FAIL - the sale went through. STOP AND REPORT THIS." -ForegroundColor Red }
else     { Write-Host "  PASS - refused" -ForegroundColor Green }

Write-Host "`n== TEST 2: XO with no amount must be REFUSED ==" -ForegroundColor Cyan
$r2 = Invoke-Api Post "$base/sales" (@{ lines=@(
    @{productId=$xo.id; quantity=1},
    @{productId=$xo.id; quantity=1}); paymentMethod='CARD' } | ConvertTo-Json -Depth 5)
if ($r2) { Write-Host "  FAIL - went through with no amount" -ForegroundColor Red }
else     { Write-Host "  PASS - refused" -ForegroundColor Green }

Write-Host "`n== TEST 3: 5000 and 7000 in one sale ==" -ForegroundColor Cyan
$r3 = Invoke-Api Post "$base/sales" (@{ lines=@(
    @{productId=$xo.id; quantity=1; unitPriceMinorUnits=5000},
    @{productId=$xo.id; quantity=1; unitPriceMinorUnits=7000});
    paymentMethod='CASH'; cashTenderedMinorUnits=12000 } | ConvertTo-Json -Depth 5)
if ($r3 -and $r3.totalMinorUnits -eq 12000 -and $r3.changeGivenMinorUnits -eq 0) {
  Write-Host ("  PASS - total {0}, change {1}" -f $r3.totalMinorUnits, $r3.changeGivenMinorUnits) -ForegroundColor Green
  $r3.lines | Format-Table name, unitPrice, quantity, lineTotal
} else {
  Write-Host "  FAIL - see above" -ForegroundColor Red
}

Write-Host "`n== PRICE_OVERRIDE audit rows ==" -ForegroundColor Cyan
Invoke-Api Get "$base/audit?type=PRICE_OVERRIDE" | Select-Object -First 5 |
  Format-Table timestamp, amountMinorUnits, detail
