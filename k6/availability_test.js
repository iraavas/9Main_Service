import http from 'k6/http';
import { check } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {
        readers: {
            executor: 'constant-vus',
            vus: __ENV.VUS_READ ? parseInt(__ENV.VUS_READ) : 10,
            duration: '1m',
            exec: 'readScenario',
        },
        writers: {
            executor: 'constant-vus',
            vus: __ENV.VUS_WRITE ? parseInt(__ENV.VUS_WRITE) : 5,
            duration: '1m',
            exec: 'writeScenario',
        },
    },
};

const baseUrl = __ENV.MAIN_SERVICE_URL || 'http://hl2.zil:8081';
const additionalUrl = __ENV.ADDITIONAL_SERVICE_URL || 'http://hl2.zil:8082';
const timeout = '360s';

const specializations = [
    'Терапевт', 'Хирург', 'Кардиолог', 'Невролог',
    'Офтальмолог', 'Отоларинголог', 'Стоматолог',
];

const diagnosisMap = {
    'Терапевт': ['ОРВИ', 'Гастрит', 'Гипертония'],
    'Хирург': ['Аппендицит', 'Грыжа', 'Травма'],
    'Кардиолог': ['Аритмия', 'ИБС', 'Гипертония'],
    'Невролог': ['Мигрень', 'Радикулит'],
    'Офтальмолог': ['Катаракта', 'Близорукость'],
    'Отоларинголог': ['Отит', 'Синусит'],
    'Стоматолог': ['Кариес', 'Пульпит'],
};

// Глобальная дата, которая будет сдвигаться
let baseDate = new Date('2025-04-23');

function getNextDateOnly() {
    const nextDate = new Date(baseDate);
    baseDate.setDate(baseDate.getDate() + 1);
    return nextDate.toISOString().split('T')[0]; // YYYY-MM-DD
}

function getNextDateTime() {
    const date = getNextDateOnly();
    const hour = String(8 + Math.floor(Math.random() * 10)).padStart(2, '0');
    const minute = String(Math.floor(Math.random() * 60)).padStart(2, '0');
    return `${date}T${hour}:${minute}:00`;
}

function fetchAll(url) {
    const res = http.get(url, { timeout });
    return res.status === 200 ? JSON.parse(res.body) : [];
}

export function readScenario() {
    const spec = randomItem(specializations);
    const date = getNextDateOnly();
    const url = `${additionalUrl}/availability/check?specialization=${encodeURIComponent(spec)}&date=${date}`;

    const res = http.get(url, { timeout });

    check(res, {
        'GET /availability/check — 200': (r) => r.status === 200,
    });
}

export function writeScenario() {
    const patients = fetchAll(`${baseUrl}/patients`);
    const doctors = fetchAll(`${baseUrl}/doctors`);

    if (patients.length === 0 || doctors.length === 0) {
        console.error('Нет пациентов или врачей');
        return;
    }

    const doctor = randomItem(doctors);
    const patient = randomItem(patients);
    const specialization = doctor.specialization;
    const diagnosis = randomItem(diagnosisMap[specialization] || ['Обследование']);
    const appointmentDate = getNextDateTime();
    const date = appointmentDate.split('T')[0];

    const checkUrl = `${additionalUrl}/availability/check?specialization=${encodeURIComponent(specialization)}&date=${date}`;
    const availabilityRes = http.get(checkUrl, { timeout });

    if (availabilityRes.status !== 200) {
        console.error(`Ошибка запроса /availability/check`);
        return;
    }

    const availableDoctors = JSON.parse(availabilityRes.body);
    const isAvailable = availableDoctors.some(d => d.id === doctor.id);

    if (!isAvailable) {
        //console.warn(`Доктор ${doctor.id} недоступен на ${appointmentDate}`);
        return;
    }

    const payload = JSON.stringify({
        patientId: patient.id,
        doctorId: doctor.id,
        appointmentDate,
        diagnosis,
        specialization
    });

    const res = http.post(`${baseUrl}/appointments`, payload, {
        headers: { 'Content-Type': 'application/json' },
        timeout,
    });

    check(res, {
        'POST /appointments — 201 or 200 or 400 or 409': (r) =>
            r.status === 201 ||
            r.status === 200 ||
            r.status === 400 ||  // например, если врач не найден
            r.status === 409     // если врач занят
    });

}