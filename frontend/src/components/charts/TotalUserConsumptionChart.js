import React, { useState } from 'react';
import { Line, Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend);

const TotalUserConsumptionChart = ({ devices }) => {
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
    const [chartType, setChartType] = useState('line');
    const [totalData, setTotalData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchTotalConsumption = async (date) => {
        if (!devices?.length) {
            setError('No devices assigned');
            return;
        }

        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');

            // Iterație și apeluri paralele către Monitoring Service pentru fiecare dispozitiv
            const results = await Promise.all(
                devices.map(device =>
                    fetch(`http://localhost/api/monitoring/devices/${device.deviceId}/consumption/daily?date=${date}`, {
                        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                    })
                        .then(r => r.ok ? r.json() : null)
                        .catch(() => null)
                )
            );

            const validResults = results.filter(r => r !== null);
            if (!validResults.length) {
                setError('No consumption data found for this date across all devices. Try another date.');
                setTotalData(null);
                return;
            }

            // Agregare pe oră
            const hourlyData = Array.from({ length: 24 }, (_, hour) => {
                let totalConsumption = 0;
                let totalMeasurements = 0;

                validResults.forEach(deviceData => {
                    const hourData = deviceData.hourlyData.find(h => h.hour === hour);
                    if (hourData) {
                        totalConsumption += hourData.consumption;
                        totalMeasurements += hourData.measurementCount;
                    }
                });

                return { hour, consumption: totalConsumption, measurementCount: totalMeasurements };
            });

            setTotalData({
                hourlyData,
                totalDailyConsumption: hourlyData.reduce((sum, h) => sum + h.consumption, 0),
                deviceCount: validResults.length
            });

        } catch (err) {
            setError('Failed to fetch consumption data');
            setTotalData(null);
        } finally {
            setLoading(false);
        }
    };

    React.useEffect(() => {
        fetchTotalConsumption(selectedDate);
    }, [devices]);

    const getChartData = () => {
        if (!totalData?.hourlyData) return null;

        return {
            labels: totalData.hourlyData.map(h => `${h.hour}:00`),
            datasets: [{
                label: 'Total Energy Consumption (kWh)',
                data: totalData.hourlyData.map(h => h.consumption),
                borderColor: 'rgb(255, 99, 132)',
                backgroundColor: chartType === 'bar' ? 'rgba(255, 99, 132, 0.6)' : 'rgba(255, 99, 132, 0.2)',
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
                text: `Total Hourly Energy Consumption - All Devices (${devices?.length || 0})`,
                font: { size: 16 },
                color: '#333' // Setat culoarea titlului pentru fundal alb
            },
            tooltip: {
                callbacks: {
                    label: (context) => {
                        const hour = totalData.hourlyData[context.dataIndex];
                        return [
                            `Total: ${hour.consumption.toFixed(3)} kWh`,
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
                title: { display: true, text: 'Total Energy Consumption (kWh)', color: '#666' },
                beginAtZero: true,
                ticks: { color: '#666' }
            }
        }
    };

    const getPeakHour = () => {
        if (!totalData?.hourlyData?.length) return 'N/A';
        const peak = totalData.hourlyData.reduce((max, h) => h.consumption > max.consumption ? h : max);
        return `${peak.hour}:00 (${peak.consumption.toFixed(3)} kWh)`;
    };

    const getAverage = () => {
        if (!totalData?.hourlyData?.length) return '0.000';
        const withData = totalData.hourlyData.filter(h => h.consumption > 0);
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
                        onChange={(e) => { setSelectedDate(e.target.value); fetchTotalConsumption(e.target.value); }}
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

            {!loading && !error && totalData && (
                <>
                    <div className="chart-summary-grid">
                        {/* Summary cards with adjusted colors for Total Consumption Chart (pink gradient) */}
                        <div className="chart-summary-card chart-summary-card-pink">
                            <div className="chart-summary-label">Total Daily</div>
                            <div className="chart-summary-value">{totalData.totalDailyConsumption.toFixed(3)} kWh</div>
                        </div>
                        <div className="chart-summary-card chart-summary-card-pink">
                            <div className="chart-summary-label">Peak Hour</div>
                            <div className="chart-summary-value">{getPeakHour()}</div>
                        </div>
                        <div className="chart-summary-card chart-summary-card-pink">
                            <div className="chart-summary-label">Average/Hour</div>
                            <div className="chart-summary-value">{getAverage()} kWh</div>
                        </div>
                        <div className="chart-summary-card chart-summary-card-pink">
                            <div className="chart-summary-label">Active Devices</div>
                            <div className="chart-summary-value">{totalData.deviceCount}/{devices?.length || 0}</div>
                        </div>
                    </div>

                    <div className="chart-wrapper">
                        {chartType === 'line' ? (
                            <Line data={getChartData()} options={chartOptions} />
                        ) : (
                            <Bar data={getChartData()} options={chartOptions} />
                        )}
                    </div>

                    {/*<div className="chart-table-container">*/}
                    {/*    <h3>Hourly Breakdown - All Devices</h3>*/}
                    {/*    <table className="table">*/}
                    {/*        <thead>*/}
                    {/*        <tr>*/}
                    {/*            <th>Hour</th>*/}
                    {/*            <th>Total (kWh)</th>*/}
                    {/*            <th>Measurements</th>*/}
                    {/*        </tr>*/}
                    {/*        </thead>*/}
                    {/*        <tbody>*/}
                    {/*        {totalData.hourlyData.map((hour) => (*/}
                    {/*            <tr key={hour.hour}>*/}
                    {/*                <td>{hour.hour}:00 - {hour.hour + 1}:00</td>*/}
                    {/*                <td>{hour.consumption.toFixed(3)}</td>*/}
                    {/*                <td>{hour.measurementCount}</td>*/}
                    {/*            </tr>*/}
                    {/*        ))}*/}
                    {/*        </tbody>*/}
                    {/*    </table>*/}
                    {/*</div>*/}

                    <div className="chart-devices-list">
                        <h4 style={{ marginBottom: '10px' }}>Included Devices:</h4>
                        {devices.map(d => (
                            <div key={d.deviceId} className="chart-device-item">
                                📱 **{d.name}** - {d.location} (Max: {d.maximumConsumption} kWh)
                            </div>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
};

export default TotalUserConsumptionChart;