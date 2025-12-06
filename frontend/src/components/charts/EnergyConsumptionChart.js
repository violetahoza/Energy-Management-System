import React, { useState } from 'react';
import { Line, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend);

const EnergyConsumptionChart = ({ device }) => {
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
    const [chartType, setChartType] = useState('line');
    const [consumptionData, setConsumptionData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchConsumption = async (date) => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(
                `http://localhost/api/monitoring/devices/${device.deviceId}/consumption/daily?date=${date}`,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                }
            );

            if (!response.ok) {
                throw new Error(response.status === 404 ? 'No consumption data found for this date. Try another date.' : 'Failed to fetch consumption data');
            }

            setConsumptionData(await response.json());
        } catch (err) {
            setError(err.message);
            setConsumptionData(null);
        } finally {
            setLoading(false);
        }
    };

    React.useEffect(() => {
        fetchConsumption(selectedDate);
    }, []);

    const getChartData = () => {
        if (!consumptionData?.hourlyData) return null;

        return {
            labels: consumptionData.hourlyData.map(h => `${h.hour}:00`),
            datasets: [{
                label: 'Energy Consumption (kWh)',
                data: consumptionData.hourlyData.map(h => h.consumption),
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: chartType === 'bar' ? 'rgba(75, 192, 192, 0.6)' : 'rgba(75, 192, 192, 0.2)',
                borderWidth: 2,
                tension: 0.4,
                fill: true,
            }]
        };
    };

    const chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { position: 'top' },
            title: {
                display: true,
                text: `Hourly Energy Consumption - ${device.name}`,
                font: { size: 16 },
                color: '#333'
            },
            tooltip: {
                callbacks: {
                    label: (context) => {
                        const hour = consumptionData.hourlyData[context.dataIndex];
                        return [
                            `Consumption: ${hour.consumption.toFixed(3)} kWh`,
                            `Measurements: ${hour.measurementCount}`
                        ];
                    }
                }
            }
        },
        scales: {
            x: {
                title: { display: true, text: 'Hour of Day', color: '#666' },
                ticks: { color: '#666' }
            },
            y: {
                title: { display: true, text: 'Energy Consumption (kWh)', color: '#666' },
                beginAtZero: true,
                ticks: { color: '#666' }
            }
        }
    };

    const getPeakHour = () => {
        if (!consumptionData?.hourlyData?.length) return 'N/A';
        const peak = consumptionData.hourlyData.reduce((max, h) => h.consumption > max.consumption ? h : max);
        return `${peak.hour}:00 (${peak.consumption.toFixed(3)} kWh)`;
    };

    const getAverage = () => {
        if (!consumptionData?.hourlyData?.length) return '0.000';
        const withData = consumptionData.hourlyData.filter(h => h.consumption > 0);
        if (!withData.length) return '0.000';
        return (withData.reduce((sum, h) => sum + h.consumption, 0) / withData.length).toFixed(3);
    };

    return (
        <div className="chart-container">
            <div className="chart-controls">
                <div className="chart-control-group">
                    <label className="form-label">Select Date:</label>
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => { setSelectedDate(e.target.value); fetchConsumption(e.target.value); }}
                        max={new Date().toISOString().split('T')[0]}
                        className="form-input"
                    />
                </div>
                <div className="chart-control-group">
                    <label className="form-label">Chart Type:</label>
                    <div className="chart-type-buttons">
                        <button
                            className={`btn btn-sm ${chartType === 'line' ? 'btn-primary' : 'btn-secondary'}`}
                            onClick={() => setChartType('line')}
                        >
                            📈 Line
                        </button>
                        <button
                            className={`btn btn-sm ${chartType === 'bar' ? 'btn-primary' : 'btn-secondary'}`}
                            onClick={() => setChartType('bar')}
                        >
                            📊 Bar
                        </button>
                    </div>
                </div>
            </div>

            {error && <div className="chart-error">⚠️ {error}</div>}
            {loading && <div className="chart-loading">Loading...</div>}

            {!loading && !error && consumptionData && (
                <>
                    <div className="chart-summary-grid">
                        <div className="chart-summary-card">
                            <div className="chart-summary-label">Total Daily</div>
                            <div className="chart-summary-value">{consumptionData.totalDailyConsumption.toFixed(3)} kWh</div>
                        </div>
                        <div className="chart-summary-card">
                            <div className="chart-summary-label">Peak Hour</div>
                            <div className="chart-summary-value">{getPeakHour()}</div>
                        </div>
                        <div className="chart-summary-card">
                            <div className="chart-summary-label">Average/Hour</div>
                            <div className="chart-summary-value">{getAverage()} kWh</div>
                        </div>
                    </div>

                    <div className="chart-wrapper">
                        {chartType === 'line' ? (
                            <Line data={getChartData()} options={chartOptions} />
                        ) : (
                            <Bar data={getChartData()} options={chartOptions} />
                        )}
                    </div>
                </>
            )}
        </div>
    );
};

export default EnergyConsumptionChart;