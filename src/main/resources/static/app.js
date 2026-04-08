
    let ws;
    let vehicles = [];
    const canvas = document.getElementById('map');
    const ctx = canvas.getContext('2d');
    let animationFrame = null; // 用于控制动画循环
    let isRunning = false; // 仿真运行状态

    function connect() {
    ws = new WebSocket('ws://localhost:8080/ws/sim');

    ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    vehicles = data.vehicles || [];
    // 仅当运行时才触发渲染（避免暂停时处理多余数据）
    if (isRunning) render();
};
}

    function render() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 画道路
    ctx.strokeStyle = '#ccc';
    ctx.lineWidth = 20;
    ctx.beginPath();
    ctx.moveTo(0, 100);
    ctx.lineTo(800, 100);
    ctx.stroke();

    // 画车辆
    vehicles.forEach(v => {
    ctx.fillStyle = 'blue';
    ctx.fillRect(v.x - 5, v.y - 3, 10, 6);
});

    // 仅当运行时继续动画循环
    if (isRunning) {
    animationFrame = requestAnimationFrame(render);
} else {
    // 暂停时显示状态提示
    ctx.font = '20px Arial';
    ctx.fillStyle = 'red';
    ctx.fillText('仿真已暂停', canvas.width/2 - 60, canvas.height/2);
}
}

    // 启动仿真
    function startSimulation() {
    fetch('/api/sim/start', {method: 'POST'})
        .then(() => {
            isRunning = true;
            // 清除可能的暂停提示
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            // 启动动画循环
            animationFrame = requestAnimationFrame(render);
        });
}

    // 暂停仿真
    function stopSimulation() {
    fetch('/api/sim/stop', {method: 'POST'})
        .then(() => {
            isRunning = false;
            // 取消动画循环
            if (animationFrame) {
                cancelAnimationFrame(animationFrame);
                animationFrame = null;
            }
            // 立即渲染暂停状态
            render();
        });
}

    // 初始化
    window.onload = () => {
    connect();

    // 设置按钮事件（使用 addEventListener 代替 onclick）
    document.getElementById('startBtn')
    .addEventListener('click', startSimulation);

    document.getElementById('stopBtn')
    .addEventListener('click', stopSimulation);

    // 初始状态：暂停
    isRunning = false;
    render();
};
