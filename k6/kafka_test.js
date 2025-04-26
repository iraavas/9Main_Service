import { check } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import kafka from 'k6/x/kafka';
import http from 'k6/http';

const producer = new kafka.Producer({
    brokers: [__ENV.KAFKA_BROKER || 'localhost:9092'],
    clientId: 'k6-producer',
});

const topic = __ENV.KAFKA_TOPIC || 'module1-topic';

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

let baseDate = new Date('2025-04-23');

function getNextDateOnly() {
    const nextDate = new Date(baseDate);
    baseDate.setDate(baseDate.getDate() + 1);
    return nextDate.toISOString().split('T')[0];
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
        return;
    }

    const payload = {
        patientId: patient.id,
        doctorId: doctor.id,
        appointmentDate,
        diagnosis,
        specialization
    };

    const kafkaMessage = {
        entity: "APPOINTMENT",
        operation: "POST",
        payload: payload
    };

    try {
        producer.produce({
            topic: topic,
            messages: [{ value: JSON.stringify(kafkaMessage) }],
        });
    } catch (err) {
        console.error(`Kafka send error: ${err}`);
    }
}
