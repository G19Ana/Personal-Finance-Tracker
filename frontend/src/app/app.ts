import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  private http = inject(HttpClient);
  private base = 'http://localhost:8080/api/finance';

  @ViewChild('catCanvas') catRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('trendCanvas') trendRef!: ElementRef<HTMLCanvasElement>;

  dash = signal<any>(null);

  newTx = { date: new Date().toISOString().substring(0, 10), description: '', amount: 0, category: 'FOOD', type: 'EXPENSE' };
  newBudget = { category: 'FOOD', monthlyLimit: 0 };
  newGoal = { name: '', targetAmount: 0, currentAmount: 0, targetDate: '' };

  private catChart?: any;
  private trendChart?: any;

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.http.get<any>(`${this.base}/dashboard`).subscribe(d => {
      this.dash.set(d);
      setTimeout(() => {
        this.renderCatChart(d.categorySpending);
        this.renderTrendChart(d.trends);
      }, 50);
    });
  }

  saveTx(): void {
    if (!this.newTx.description || this.newTx.amount <= 0) return;
    this.http.post(`${this.base}/transactions`, this.newTx).subscribe(() => {
      this.newTx.description = '';
      this.newTx.amount = 0;
      this.refresh();
    });
  }

  deleteTx(id: number): void {
    this.http.delete(`${this.base}/transactions/${id}`).subscribe(() => this.refresh());
  }

  saveBudget(): void {
    if (this.newBudget.monthlyLimit <= 0) return;
    this.http.post(`${this.base}/budgets`, this.newBudget).subscribe(() => {
      this.newBudget.monthlyLimit = 0;
      this.refresh();
    });
  }

  saveGoal(): void {
    if (!this.newGoal.name || this.newGoal.targetAmount <= 0) return;
    this.http.post(`${this.base}/goals`, this.newGoal).subscribe(() => {
      this.newGoal = { name: '', targetAmount: 0, currentAmount: 0, targetDate: '' };
      this.refresh();
    });
  }

  updateGoalDeposit(g: any, amt: number): void {
    const updated = Number(g.currentAmount) + Number(amt);
    this.http.patch(`${this.base}/goals/${g.id}`, { currentAmount: updated }).subscribe(() => this.refresh());
  }

  deleteGoal(id: number): void {
    this.http.delete(`${this.base}/goals/${id}`).subscribe(() => this.refresh());
  }

  uploadCsv(event: any): void {
    const file = event.target.files[0];
    if (!file) return;
    const form = new FormData();
    form.append('file', file);
    this.http.post(`${this.base}/import-csv`, form).subscribe(() => this.refresh());
  }

  private renderCatChart(catMap: any): void {
    if (!this.catRef?.nativeElement) return;
    if (this.catChart) this.catChart.destroy();

    const labels = Object.keys(catMap);
    const data = Object.values(catMap);

    this.catChart = new Chart(this.catRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: ['#2563eb', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#6b7280']
        }]
      },
      options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
    });
  }

  private renderTrendChart(trends: any[]): void {
    if (!this.trendRef?.nativeElement) return;
    if (this.trendChart) this.trendChart.destroy();

    const labels = trends.map(t => t.month);
    const income = trends.map(t => t.income);
    const expense = trends.map(t => t.expense);

    this.trendChart = new Chart(this.trendRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: 'Income', data: income, backgroundColor: '#10b981' },
          { label: 'Expense', data: expense, backgroundColor: '#ef4444' }
        ]
      },
      options: { responsive: true, scales: { y: { beginAtZero: true } } }
    });
  }
}
