import argparse
import json
import random
from datetime import datetime, timedelta

from faker import Faker
from kafka import KafkaProducer

fake = Faker("ru_RU")

# Kafka-продюсер
producer = KafkaProducer(
    bootstrap_servers=["127.0.0.2:9094"],
    value_serializer=lambda v: json.dumps(v).encode("utf-8")
)

SPECIALIZATIONS = [
    "Терапевт", "Хирург", "Кардиолог", "Невролог",
    "Офтальмолог", "Отоларинголог", "Стоматолог"
]

DIAGNOSIS_MAP = {
    "Терапевт": ["ОРВИ", "Гастрит", "Гипертония"],
    "Хирург": ["Аппендицит", "Грыжа", "Травма"],
    "Кардиолог": ["Аритмия", "ИБС", "Гипертония"],
    "Невролог": ["Мигрень", "Радикулит"],
    "Офтальмолог": ["Катаракта", "Близорукость"],
    "Отоларинголог": ["Отит", "Синусит"],
    "Стоматолог": ["Кариес", "Пульпит"]
}

# Генерация данных

def generate_patient():
    gender = random.choice(["male", "female"])
    fio = fake.name_male() if gender == "male" else fake.name_female()
    date_of_birth = fake.date_of_birth(minimum_age=20, maximum_age=80).isoformat()
    insurance_number = fake.unique.bothify(text="#### ######")
    return {
        "entity": "PATIENT",
        "operation": "POST",
        "payload": {
            "fio": fio,
            "dateOfBirth": date_of_birth,
            "insuranceNumber": insurance_number
        }
    }

def generate_doctor():
    specialization = random.choice(SPECIALIZATIONS)
    fio = fake.name_male()
    work_schedule = f"{random.choice(['Пн-Пт', 'Пн-Ср', 'Вт-Чт'])} {random.randint(8,10)}:00-{random.randint(16,19)}:00"
    return {
        "entity": "DOCTOR",
        "operation": "POST",
        "payload": {
            "fio": fio,
            "specialization": specialization,
            "workSchedule": work_schedule
        }
    }

def generate_appointment(patient_id, doctor_id, specialization):
    appointment_date = (datetime.now() + timedelta(days=random.randint(1, 30))).strftime("%Y-%m-%dT%H:%M:%S")
    diagnosis = random.choice(DIAGNOSIS_MAP.get(specialization, ["Обследование"]))
    return {
        "entity": "APPOINTMENT",
        "operation": "POST",
        "payload": {
            "patientId": patient_id,
            "doctorId": doctor_id,
            "appointmentDate": appointment_date,
            "diagnosis": diagnosis,
            "specialization": specialization
        }
    }

def generate_delete(entity, entity_id):
    return {
        "entity": entity,
        "operation": "DELETE",
        "payload": {
            "id": entity_id
        }
    }

# Основная функция

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--entity", required=True, choices=["PATIENT", "DOCTOR", "APPOINTMENT"])
    parser.add_argument("--operation", default="POST", choices=["POST", "DELETE"])
    parser.add_argument("--count", type=int, default=10)
    parser.add_argument("--id", type=int, help="ID для DELETE операции")
    parser.add_argument("--patient_id", type=int, help="ID пациента для APPOINTMENT")
    parser.add_argument("--doctor_id", type=int, help="ID врача для APPOINTMENT")
    parser.add_argument("--specialization", type=str, help="Специализация врача для APPOINTMENT")

    args = parser.parse_args()
    topic = "var02"

    if args.operation == "POST":
        for _ in range(args.count):
            if args.entity == "PATIENT":
                producer.send(topic, generate_patient())
            elif args.entity == "DOCTOR":
                producer.send(topic, generate_doctor())
            elif args.entity == "APPOINTMENT":
                if args.patient_id is None or args.doctor_id is None or args.specialization is None:
                    print("Для APPOINTMENT нужно указать --patient_id, --doctor_id и --specialization")
                    return
                producer.send(topic, generate_appointment(args.patient_id, args.doctor_id, args.specialization))

        print(f"Отправлено {args.count} сообщений типа {args.entity} в Kafka")

    elif args.operation == "DELETE":
        if args.id is None:
            print("Ошибка: для DELETE нужно указать --id")
            return
        producer.send(topic, generate_delete(args.entity, args.id))
        print(f"Отправлено сообщение DELETE для {args.entity} с ID={args.id}")

    producer.flush()

if __name__ == "__main__":
    main()
