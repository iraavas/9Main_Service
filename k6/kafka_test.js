import { check } from 'k6';
import http from 'k6/http';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { Writer, SCHEMA_TYPE_STRING, SchemaRegistry } from 'k6/x/kafka';

const topic = 'var02_';
const brokers = ['10.60.3.27:9094', '10.60.3.28:9094'];

const writer = new Writer({
    brokers: brokers,
    topic: topic,
});

const schemaRegistry = new SchemaRegistry();

export const options = {
    scenarios: {
        writers: {
            executor: 'constant-vus',
            vus: __ENV.VUS_WRITE ? parseInt(__ENV.VUS_WRITE) : 5,
            duration: '1m',
            exec: 'writeScenario',
        },
        readers: {
            executor: 'constant-vus',
            vus: __ENV.VUS_READ ? parseInt(__ENV.VUS_READ) : 10,
            duration: '1m',
            exec: 'readScenario',
        },
    },
};

const baseUrl = 'http://10.60.3.4:31081';
const additionalUrl = 'http://10.60.3.4:31082';
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
    const specialization = randomItem(specializations);
    const date = getNextDateOnly();
    const url = `${additionalUrl}/availability/check?specialization=${encodeURIComponent(specialization)}&date=${date}`;

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

    const checkUrl = `${additionalUrl}/availability/check?specialization=${encodeURIComponent(specialization)}&date=${appointmentDate.split('T')[0]}`;
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
        entity: 'APPOINTMENT',
        operation: 'POST',
        payload: {
            patientId: patient.id,
            doctorId: doctor.id,
            appointmentDate,
            diagnosis,
            specialization,
        },
    };

    try {
        writer.produce({
            messages: [
                {
                    key: schemaRegistry.serialize({
                        data: patient.id.toString(),
                        schemaType: SCHEMA_TYPE_STRING,
                    }),
                    value: schemaRegistry.serialize({
                        data: JSON.stringify(payload),
                        schemaType: SCHEMA_TYPE_STRING,
                    }),
                },
            ],
        });
    } catch (err) {
        console.error(`Ошибка отправки Kafka: ${err}`);
    }
}