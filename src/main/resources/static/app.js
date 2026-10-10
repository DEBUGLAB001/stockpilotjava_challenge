let productsCache = [];
let warehousesCache = [];

document.addEventListener('DOMContentLoaded', () => {
  initMetadata();
  loadInventory();
  loadOrders();
  loadMovements();

  // Periodic polling for candidate live changes without manual restart
  setInterval(() => {
    loadInventory();
    loadOrders();
  }, 5000);
});

function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
  document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));

  const targetTab = document.getElementById(tabId);
  if (targetTab) {
    targetTab.classList.add('active');
  }

  // Highlight active button
  const buttons = document.querySelectorAll('.nav-tab');
  if (tabId === 'inventory-tab') buttons[0].classList.add('active');
  if (tabId === 'orders-tab') buttons[1].classList.add('active');
  if (tabId === 'movements-tab') buttons[2].classList.add('active');
}

async function initMetadata() {
  try {
    const [pRes, wRes] = await Promise.all([
      fetch('api/inventory/products'),
      fetch('api/inventory/warehouses')
    ]);

    productsCache = await pRes.json();
    warehousesCache = await wRes.json();

    const prodSelects = [document.getElementById('adjust-product'), document.getElementById('order-product')];
    prodSelects.forEach(sel => {
      if (!sel) return;
      sel.innerHTML = productsCache.map(p => `<option value="${p.id}">${p.name} (${p.sku})</option>`).join('');
    });

    const whSelects = [document.getElementById('adjust-warehouse'), document.getElementById('order-warehouse')];
    whSelects.forEach(sel => {
      if (!sel) return;
      sel.innerHTML = warehousesCache.map(w => `<option value="${w.id}">${w.name} (${w.code})</option>`).join('');
    });
  } catch (err) {
    console.error('Failed to load metadata', err);
  }
}

async function loadInventory() {
  try {
    const res = await fetch('api/inventory');
    if (!res.ok) throw new Error('Network error');
    const items = await res.json();

    const tbody = document.getElementById('inventory-tbody');
    if (items.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center">No inventory records found</td></tr>';
      return;
    }

    tbody.innerHTML = items.map(item => `
      <tr>
        <td><strong>${item.product.name}</strong><br><small style="color: var(--text-muted)">${item.product.sku}</small></td>
        <td>${item.warehouse.name}</td>
        <td><strong style="color: ${item.availableStock > 0 ? 'var(--accent-green)' : 'var(--accent-red)'}">${item.availableStock}</strong></td>
        <td>${item.reservedStock}</td>
        <td>$${item.product.unitPrice}</td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to load inventory', err);
  }
}

async function loadOrders() {
  try {
    const filter = document.getElementById('order-status-filter').value;
    const url = filter && filter !== 'ALL' ? `api/orders?status=${filter}` : 'api/orders';
    const res = await fetch(url);
    if (!res.ok) throw new Error('Network error');
    const orders = await res.json();

    const tbody = document.getElementById('orders-tbody');
    if (orders.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center">No orders placed yet</td></tr>';
      return;
    }

    tbody.innerHTML = orders.map(ord => `
      <tr>
        <td><strong>${ord.orderNumber}</strong></td>
        <td>${ord.customerName}</td>
        <td>${(ord.items || []).map(i => `${i.product.name} (x${i.quantity})`).join(', ')}</td>
        <td>$${ord.totalAmount}</td>
        <td><span class="badge badge-${ord.status}">${ord.status}</span></td>
        <td>
          ${ord.status !== 'CANCELLED' ? `
            <button class="btn btn-danger" onclick="cancelOrder('${ord.orderNumber}')">Cancel</button>
          ` : '<span style="color: var(--text-muted)">Cancelled</span>'}
        </td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to load orders', err);
  }
}

async function loadMovements() {
  try {
    const res = await fetch('api/inventory/movements');
    if (!res.ok) throw new Error('Network error');
    const movements = await res.json();

    const tbody = document.getElementById('movements-tbody');
    if (movements.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center">No inventory movements recorded</td></tr>';
      return;
    }

    tbody.innerHTML = movements.map(m => `
      <tr>
        <td><small>${new Date(m.timestamp).toLocaleTimeString()}</small></td>
        <td>${m.product.name}</td>
        <td>${m.warehouse.name}</td>
        <td><span class="badge badge-type">${m.type}</span></td>
        <td><strong>${m.quantity}</strong></td>
        <td>${m.referenceOrderNumber || '-'}</td>
        <td>${m.notes || '-'}</td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Failed to load movements', err);
  }
}

async function handleStockAdjust(e) {
  e.preventDefault();
  const msgBox = document.getElementById('adjust-msg');
  msgBox.className = 'notification-msg';

  const payload = {
    productId: parseInt(document.getElementById('adjust-product').value),
    warehouseId: parseInt(document.getElementById('adjust-warehouse').value),
    quantity: parseInt(document.getElementById('adjust-quantity').value),
    reason: document.getElementById('adjust-reason').value
  };

  try {
    const res = await fetch('api/inventory/adjust', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || 'Stock adjustment failed');
    }

    msgBox.textContent = 'Inventory successfully adjusted!';
    msgBox.classList.add('success');
    document.getElementById('adjust-quantity').value = '';
    loadInventory();
    loadMovements();
  } catch (err) {
    msgBox.textContent = err.message;
    msgBox.classList.add('error');
  }
}

async function handleCreateOrder(e) {
  e.preventDefault();
  const msgBox = document.getElementById('order-msg');
  msgBox.className = 'notification-msg';

  const payload = {
    customerName: document.getElementById('order-customer').value,
    items: [
      {
        productId: parseInt(document.getElementById('order-product').value),
        warehouseId: parseInt(document.getElementById('order-warehouse').value),
        quantity: parseInt(document.getElementById('order-quantity').value)
      }
    ]
  };

  try {
    const res = await fetch('api/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok || (data.status === 'error')) {
      throw new Error(data.message || 'Order creation failed');
    }

    msgBox.textContent = `Order ${data.orderNumber} successfully created & reserved!`;
    msgBox.classList.add('success');
    document.getElementById('order-customer').value = '';
    loadOrders();
    loadInventory();
    loadMovements();
  } catch (err) {
    msgBox.textContent = err.message;
    msgBox.classList.add('error');
  }
}

async function cancelOrder(orderNumber) {
  if (!confirm(`Are you sure you want to cancel order ${orderNumber}?`)) return;

  try {
    const res = await fetch(`api/orders/${orderNumber}/cancel`, { method: 'POST' });
    if (!res.ok) throw new Error('Failed to cancel order');

    loadOrders();
    loadInventory();
    loadMovements();
  } catch (err) {
    alert(err.message);
  }
}
