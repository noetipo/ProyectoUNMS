import { Injectable } from '@angular/core';
import { format, sub } from 'date-fns';

export type BudgetDetail = {
  id: string;
  type: string;
  total: number;
  expensesAmount: number;
  expensesPercentage: number;
  remainingAmount: number;
  remainingPercentage: number;
};

@Injectable({ providedIn: 'root' })
export class ProjectDashboardService {
  // Data
  private now = new Date();
  data = {
    summary: [
      {
        title: 'Tesis en proceso',
        icon: 'file-clock',
        value: 84,
        change: {
          value: 12,
          unit: '',
          period: 'desde el mes pasado',
          up: true,
        },
      },
      {
        title: 'Por sustentar',
        icon: 'calendar-clock',
        value: 18,
        change: {
          value: 5,
          unit: '',
          period: 'este mes',
          up: true,
        },
      },
      {
        title: 'Observadas',
        icon: 'triangle-alert',
        value: 9,
        change: {
          value: -3,
          unit: '',
          period: 'desde la semana pasada',
          up: true,
        },
      },
      {
        title: 'Tituladas (año)',
        icon: 'graduation-cap',
        value: 42,
        change: {
          value: 8,
          unit: '',
          period: 'este año',
          up: true,
        },
      },
    ],
    issues: {
      overview: [
        {
          label: 'Proyecto inscrito',
          value: 84,
        },
        {
          label: 'En desarrollo',
          value: 56,
        },
        {
          label: 'En revisión',
          value: 23,
        },
        {
          label: 'Observadas',
          value: 9,
        },
        {
          label: 'Aprobadas',
          value: 31,
        },
        {
          label: 'Tituladas',
          value: 42,
        },
      ],
      chart: {
        labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun'],
        series: [
          {
            name: 'Tesis inscritas',
            type: 'line',
            data: [12, 18, 15, 22, 19, 25],
          },
          {
            name: 'Sustentadas',
            type: 'column',
            data: [4, 6, 5, 9, 7, 11],
          },
        ],
      },
    },
    taskDistribution: {
      chart: {
        labels: ['Cardiología', 'Pediatría', 'Cirugía', 'Medicina Interna', 'Ginecología'],
        series: [22.5, 18.0, 15.5, 28.0, 16.0],
      },
    },
    schedule: [
      {
        title: 'Sustentación: Factores de riesgo cardiovascular en adultos mayores',
        date: this.now,
        time: '09:00 AM',
        location: 'Auditorio A — Posgrado',
      },
      {
        title: 'Sustentación: Prevalencia de diabetes tipo 2 en zonas rurales',
        date: this.now,
        time: '11:00 AM',
        location: 'Sala de grados 2',
      },
      {
        title: 'Revisión de jurado: Manejo de sepsis neonatal',
        date: this.now,
        time: '12:30 PM',
        location: 'Aula 305',
      },
      {
        title: 'Sustentación: Eficacia de la telemedicina en control prenatal',
        date: this.now,
        time: '03:00 PM',
        location: 'Auditorio B',
      },
      {
        title: 'Asesoría: Resistencia antimicrobiana en UCI',
        date: this.now,
        time: '05:00 PM',
        location: 'Consultorio docente',
      },
    ],
    budget: [
      {
        id: '1',
        type: 'Maestría en Medicina',
        total: 96,
        expensesAmount: 60,
        expensesPercentage: 62.5,
        remainingAmount: 36,
        remainingPercentage: 37.5,
      },
      {
        id: '2',
        type: 'Doctorado en Medicina',
        total: 54,
        expensesAmount: 38,
        expensesPercentage: 70.4,
        remainingAmount: 16,
        remainingPercentage: 29.6,
      },
      {
        id: '3',
        type: 'Esp. Cardiología',
        total: 28,
        expensesAmount: 18,
        expensesPercentage: 64.3,
        remainingAmount: 10,
        remainingPercentage: 35.7,
      },
      {
        id: '4',
        type: 'Esp. Pediatría',
        total: 34,
        expensesAmount: 22,
        expensesPercentage: 64.7,
        remainingAmount: 12,
        remainingPercentage: 35.3,
      },
      {
        id: '5',
        type: 'Esp. Cirugía General',
        total: 33,
        expensesAmount: 25,
        expensesPercentage: 75.8,
        remainingAmount: 8,
        remainingPercentage: 24.2,
      },
    ],
    budgetDistribution: {
      categories: ['Concept', 'Design', 'Development', 'Extras', 'Marketing'],
      series: [
        {
          name: 'Budget',
          data: [12, 20, 28, 15, 25],
        },
      ],
    },
    weeklyExpenses: {
      amount: 17663,
      labels: [
        format(sub(this.now, { days: 47 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 40 }), 'dd MMM'),
        format(sub(this.now, { days: 39 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 32 }), 'dd MMM'),
        format(sub(this.now, { days: 31 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 24 }), 'dd MMM'),
        format(sub(this.now, { days: 23 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 16 }), 'dd MMM'),
        format(sub(this.now, { days: 15 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 8 }), 'dd MMM'),
        format(sub(this.now, { days: 7 }), 'dd MMM') +
          ' - ' +
          format(this.now, 'dd MMM'),
      ],
      series: [
        {
          name: 'Expenses',
          data: [4412, 4345, 4541, 4677, 4322, 4123],
        },
      ],
    },
    monthlyExpenses: {
      amount: 54663,
      labels: [
        format(sub(this.now, { days: 31 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 24 }), 'dd MMM'),
        format(sub(this.now, { days: 23 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 16 }), 'dd MMM'),
        format(sub(this.now, { days: 15 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 8 }), 'dd MMM'),
        format(sub(this.now, { days: 7 }), 'dd MMM') +
          ' - ' +
          format(this.now, 'dd MMM'),
      ],
      series: [
        {
          name: 'Expenses',
          data: [15521, 15519, 15522, 15521],
        },
      ],
    },
    yearlyExpenses: {
      amount: 648813,
      labels: [
        format(sub(this.now, { days: 79 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 72 }), 'dd MMM'),
        format(sub(this.now, { days: 71 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 64 }), 'dd MMM'),
        format(sub(this.now, { days: 63 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 56 }), 'dd MMM'),
        format(sub(this.now, { days: 55 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 48 }), 'dd MMM'),
        format(sub(this.now, { days: 47 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 40 }), 'dd MMM'),
        format(sub(this.now, { days: 39 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 32 }), 'dd MMM'),
        format(sub(this.now, { days: 31 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 24 }), 'dd MMM'),
        format(sub(this.now, { days: 23 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 16 }), 'dd MMM'),
        format(sub(this.now, { days: 15 }), 'dd MMM') +
          ' - ' +
          format(sub(this.now, { days: 8 }), 'dd MMM'),
        format(sub(this.now, { days: 7 }), 'dd MMM') +
          ' - ' +
          format(this.now, 'dd MMM'),
      ],
      series: [
        {
          name: 'Expenses',
          data: [
            45891, 45801, 45834, 45843, 45800, 45900, 45814, 45856, 45910,
            45849,
          ],
        },
      ],
    },
    teamMembers: [
      {
        id: '2bfa2be5-7688-48d5-b5ac-dc0d9ac97f14',
        avatar: 'images/users/female-10.jpg',
        name: 'Nadia Mcknight',
        email: 'nadiamcknight@mail.com',
        phone: '+1-943-511-2203',
        title: 'Project Director',
      },
      {
        id: '77a4383b-b5a5-4943-bc46-04c3431d1566',
        avatar: 'images/users/male-19.jpg',
        name: 'Best Blackburn',
        email: 'blackburn.best@beadzza.me',
        phone: '+1-814-498-3701',
        title: 'Senior Developer',
      },
      {
        id: '8bb0f597-673a-47ca-8c77-2f83219cb9af',
        avatar: 'images/users/male-14.jpg',
        name: 'Duncan Carver',
        email: 'duncancarver@mail.info',
        phone: '+1-968-547-2111',
        title: 'Senior Developer',
      },
      {
        id: 'c318e31f-1d74-49c5-8dae-2bc5805e2fdb',
        avatar: 'images/users/male-01.jpg',
        name: 'Martin Richards',
        email: 'martinrichards@mail.biz',
        phone: '+1-902-500-2668',
        title: 'Junior Developer',
      },
      {
        id: '0a8bc517-631a-4a93-aacc-000fa2e8294c',
        avatar: 'images/users/female-20.jpg',
        name: 'Candice Munoz',
        email: 'candicemunoz@mail.co.uk',
        phone: '+1-838-562-2769',
        title: 'Lead Designer',
      },
      {
        id: 'a4c9945a-757b-40b0-8942-d20e0543cabd',
        avatar: 'images/users/female-01.jpg',
        name: 'Vickie Mosley',
        email: 'vickiemosley@mail.net',
        phone: '+1-939-555-3054',
        title: 'Designer',
      },
      {
        id: 'b8258ccf-48b5-46a2-9c95-e0bd7580c645',
        avatar: 'images/users/female-02.jpg',
        name: 'Tina Harris',
        email: 'tinaharris@mail.ca',
        phone: '+1-933-464-2431',
        title: 'Designer',
      },
      {
        id: 'f004ea79-98fc-436c-9ba5-6cfe32fe583d',
        avatar: 'images/users/male-02.jpg',
        name: 'Holt Manning',
        email: 'holtmanning@mail.org',
        phone: '+1-822-531-2600',
        title: 'Marketing Manager',
      },
      {
        id: '8b69fe2d-d7cc-4a3d-983d-559173e37d37',
        avatar: 'images/users/female-03.jpg',
        name: 'Misty Ramsey',
        email: 'mistyramsey@mail.us',
        phone: '+1-990-457-2106',
        title: 'Consultant',
      },
    ],
  };
}
