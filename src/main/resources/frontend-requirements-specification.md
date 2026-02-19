# EduFlow Frontend Requirements Specification
## Angular 17+ Application for School Fees & Academic Management System

---

## 1. PROJECT OVERVIEW

### 1.1 Purpose
Build a modern, responsive Angular frontend application that integrates with the EduFlow Spring Boot backend API. The application will serve four user roles: Admin, Teacher, Parent, and Student.

### 1.2 Technology Stack
- **Framework:** Angular 17+ (standalone components)
- **UI Library:** Angular Material or PrimeNG
- **State Management:** NgRx or Angular Signals
- **HTTP Client:** Angular HttpClient with interceptors
- **Authentication:** JWT with refresh token rotation
- **Styling:** SCSS with CSS variables for theming
- **Charts:** Chart.js or ng2-charts for dashboards
- **Forms:** Reactive Forms with validation
- **Routing:** Angular Router with guards

### 1.3 Backend API Base URL
```
http://localhost:8080/api
```

---

## 2. AUTHENTICATION & AUTHORIZATION

### 2.1 JWT Token Management
The backend returns tokens in this format:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "email": "admin@eduflow.com",
    "firstName": "System",
    "lastName": "Administrator",
    "fullName": "System Administrator",
    "roles": ["ADMIN"]
  }
}
```

### 2.2 Token Storage Strategy
- Store `accessToken` in memory (variable/signal)
- Store `refreshToken` in `localStorage` or `httpOnly cookie`
- Implement automatic token refresh when access token expires
- Clear all tokens on logout

### 2.3 HTTP Interceptor Requirements
Create an `AuthInterceptor` that:
1. Adds `Authorization: Bearer <token>` header to all API requests
2. Excludes auth endpoints (`/v1/auth/*`) from token injection
3. Handles 401 responses by attempting token refresh
4. Queues failed requests during token refresh
5. Redirects to login on refresh failure

### 2.4 Route Guards
Implement guards for:
- `AuthGuard` - Ensures user is authenticated
- `RoleGuard` - Ensures user has required role(s)
- `NoAuthGuard` - Prevents authenticated users from accessing login/register

---

## 3. API ENDPOINTS REFERENCE

### 3.1 Authentication Endpoints
```
POST   /v1/auth/login              - Login
POST   /v1/auth/register           - Register
POST   /v1/auth/refresh            - Refresh token
POST   /v1/auth/logout             - Logout
POST   /v1/auth/forgot-password    - Request password reset
POST   /v1/auth/reset-password     - Reset password
```

### 3.2 Admin Endpoints (Role: ADMIN)
```
# Students
GET    /v1/admin/students                    - List students (paginated)
GET    /v1/admin/students/{id}               - Get student by ID
GET    /v1/admin/students/student-id/{id}    - Get by student ID
POST   /v1/admin/students                    - Create student
PUT    /v1/admin/students/{id}               - Update student
DELETE /v1/admin/students/{id}               - Delete student
GET    /v1/admin/students/class/{classId}    - Students by class
POST   /v1/admin/students/{id}/enroll        - Enroll in class
GET    /v1/admin/students/search?name=       - Search students
GET    /v1/admin/students/status/{status}    - Filter by status

# Fees
GET    /v1/admin/fees                        - List all fees
GET    /v1/admin/fees/{id}                   - Get fee by ID
GET    /v1/admin/fees/academic-year/{year}   - Fees by year
POST   /v1/admin/fees                        - Create fee
PUT    /v1/admin/fees/{id}                   - Update fee
DELETE /v1/admin/fees/{id}                   - Delete fee
POST   /v1/admin/fees/assign                 - Assign fees to students
GET    /v1/admin/fees/student/{studentId}    - Get student fees
POST   /v1/admin/fees/assignment/{id}/discount - Apply discount
POST   /v1/admin/fees/assignment/{id}/waive  - Waive fee
```

### 3.3 Teacher Endpoints (Role: TEACHER)
```
GET    /v1/teacher/classes                   - Get assigned classes
GET    /v1/teacher/classes/{id}/students     - Students in class
POST   /v1/teacher/grades                    - Add/update grade
GET    /v1/teacher/grades?academicYear=      - Get grades
POST   /v1/teacher/homework                  - Create homework
GET    /v1/teacher/homework                  - List homework
```

### 3.4 Parent Endpoints (Role: PARENT)
```
GET    /v1/parent/children                   - Get children
GET    /v1/parent/children/{id}/fees         - Child fees
GET    /v1/parent/children/{id}/fees/{year}  - Fees by year
GET    /v1/parent/children/{id}/payments     - Payment history
POST   /v1/parent/payments                   - Make payment
GET    /v1/parent/payments/{id}              - Payment details
GET    /v1/parent/notifications              - Notifications
GET    /v1/parent/notifications/unread-count - Unread count
```

---

## 4. APPLICATION STRUCTURE

### 4.1 Module/Feature Structure
```
src/
├── app/
│   ├── core/                          # Singleton services, guards, interceptors
│   │   ├── guards/
│   │   │   ├── auth.guard.ts
│   │   │   ├── role.guard.ts
│   │   │   └── no-auth.guard.ts
│   │   ├── interceptors/
│   │   │   ├── auth.interceptor.ts
│   │   │   └── error.interceptor.ts
│   │   ├── services/
│   │   │   ├── auth.service.ts
│   │   │   ├── storage.service.ts
│   │   │   └── notification.service.ts
│   │   └── models/
│   │       ├── user.model.ts
│   │       ├── auth.model.ts
│   │       └── api-response.model.ts
│   │
│   ├── shared/                        # Shared components, pipes, directives
│   │   ├── components/
│   │   │   ├── header/
│   │   │   ├── sidebar/
│   │   │   ├── footer/
│   │   │   ├── loading-spinner/
│   │   │   ├── confirm-dialog/
│   │   │   ├── data-table/
│   │   │   ├── pagination/
│   │   │   └── stat-card/
│   │   ├── pipes/
│   │   │   ├── currency.pipe.ts
│   │   │   └── date-format.pipe.ts
│   │   └── directives/
│   │       └── has-role.directive.ts
│   │
│   ├── features/                      # Feature modules (lazy loaded)
│   │   ├── auth/
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   ├── forgot-password/
│   │   │   └── reset-password/
│   │   │
│   │   ├── admin/
│   │   │   ├── dashboard/
│   │   │   ├── students/
│   │   │   │   ├── student-list/
│   │   │   │   ├── student-form/
│   │   │   │   └── student-detail/
│   │   │   ├── classes/
│   │   │   ├── subjects/
│   │   │   ├── teachers/
│   │   │   ├── fees/
│   │   │   │   ├── fee-list/
│   │   │   │   ├── fee-form/
│   │   │   │   └── fee-assignment/
│   │   │   ├── payments/
│   │   │   ├── reports/
│   │   │   └── announcements/
│   │   │
│   │   ├── teacher/
│   │   │   ├── dashboard/
│   │   │   ├── my-classes/
│   │   │   ├── gradebook/
│   │   │   ├── homework/
│   │   │   └── reports/
│   │   │
│   │   ├── parent/
│   │   │   ├── dashboard/
│   │   │   ├── children/
│   │   │   ├── fees/
│   │   │   ├── payments/
│   │   │   │   ├── payment-history/
│   │   │   │   └── make-payment/
│   │   │   ├── grades/
│   │   │   └── notifications/
│   │   │
│   │   └── student/
│   │       ├── dashboard/
│   │       ├── grades/
│   │       ├── homework/
│   │       └── reports/
│   │
│   ├── layouts/
│   │   ├── auth-layout/               # Layout for login/register
│   │   ├── admin-layout/              # Layout with admin sidebar
│   │   ├── teacher-layout/
│   │   ├── parent-layout/
│   │   └── student-layout/
│   │
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
│
├── assets/
│   ├── images/
│   ├── icons/
│   └── i18n/                          # Translation files (optional)
│
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
│
└── styles/
    ├── _variables.scss
    ├── _mixins.scss
    ├── _typography.scss
    └── styles.scss
```

---

## 5. ROUTING CONFIGURATION

### 5.1 Route Structure
```typescript
// app.routes.ts
export const routes: Routes = [
  // Auth routes (no layout)
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes'),
    canActivate: [NoAuthGuard]
  },

  // Admin routes
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] },
    loadChildren: () => import('./features/admin/admin.routes')
  },

  // Teacher routes
  {
    path: 'teacher',
    component: TeacherLayoutComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['TEACHER'] },
    loadChildren: () => import('./features/teacher/teacher.routes')
  },

  // Parent routes
  {
    path: 'parent',
    component: ParentLayoutComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['PARENT'] },
    loadChildren: () => import('./features/parent/parent.routes')
  },

  // Student routes
  {
    path: 'student',
    component: StudentLayoutComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['STUDENT'] },
    loadChildren: () => import('./features/student/student.routes')
  },

  // Default redirect based on role
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: 'auth/login' }
];
```

---

## 6. CORE SERVICES SPECIFICATION

### 6.1 AuthService
```typescript
// core/services/auth.service.ts
@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUser = signal<User | null>(null);
  private accessToken = signal<string | null>(null);

  // Observables
  currentUser$ = toObservable(this.currentUser);
  isAuthenticated$ = computed(() => !!this.currentUser());

  // Methods
  login(credentials: LoginRequest): Observable<AuthResponse>;
  register(data: RegisterRequest): Observable<AuthResponse>;
  logout(): void;
  refreshToken(): Observable<AuthResponse>;
  forgotPassword(email: string): Observable<MessageResponse>;
  resetPassword(token: string, password: string): Observable<MessageResponse>;

  // Helpers
  hasRole(role: string): boolean;
  hasAnyRole(roles: string[]): boolean;
  getAccessToken(): string | null;
  isTokenExpired(): boolean;
}
```

### 6.2 API Service (Generic)
```typescript
// core/services/api.service.ts
@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiUrl;

  get<T>(endpoint: string, params?: HttpParams): Observable<T>;
  post<T>(endpoint: string, body: any): Observable<T>;
  put<T>(endpoint: string, body: any): Observable<T>;
  delete<T>(endpoint: string): Observable<T>;

  // Paginated requests
  getPaged<T>(endpoint: string, page: number, size: number, params?: HttpParams): Observable<PagedResponse<T>>;
}
```

### 6.3 Student Service (Example Feature Service)
```typescript
// features/admin/services/student.service.ts
@Injectable({ providedIn: 'root' })
export class StudentService {
  private endpoint = '/v1/admin/students';

  getStudents(page: number, size: number): Observable<PagedResponse<Student>>;
  getStudentById(id: number): Observable<Student>;
  getStudentByStudentId(studentId: string): Observable<Student>;
  createStudent(student: CreateStudentRequest): Observable<Student>;
  updateStudent(id: number, student: CreateStudentRequest): Observable<Student>;
  deleteStudent(id: number): Observable<MessageResponse>;
  getStudentsByClass(classId: number): Observable<Student[]>;
  enrollStudent(studentId: number, classId: number): Observable<Student>;
  searchStudents(name: string, page: number, size: number): Observable<PagedResponse<Student>>;
}
```

---

## 7. DATA MODELS / INTERFACES

### 7.1 Authentication Models
```typescript
// models/auth.model.ts
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: 'ADMIN' | 'TEACHER' | 'PARENT' | 'STUDENT';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  roles: string[];
}

export interface MessageResponse {
  message: string;
  success: boolean;
}
```

### 7.2 Student Models
```typescript
// models/student.model.ts
export interface Student {
  id: number;
  studentId: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone?: string;
  dateOfBirth?: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  enrollmentDate?: string;
  address?: string;
  bloodGroup?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'GRADUATED' | 'TRANSFERRED' | 'EXPELLED';
  currentClass?: ClassSummary;
  parent?: ParentSummary;
}

export interface CreateStudentRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  address?: string;
  bloodGroup?: string;
  medicalConditions?: string;
  parentId?: number;
  classId?: number;
}

export interface ClassSummary {
  id: number;
  name: string;
  grade: number;
  academicYear: string;
}

export interface ParentSummary {
  id: number;
  name: string;
  phone?: string;
  email?: string;
}
```

### 7.3 Fee Models
```typescript
// models/fee.model.ts
export type FeeCategory = 'TUITION' | 'TRANSPORT' | 'BOARDING' | 'EXAM' |
                          'ACTIVITY' | 'LIBRARY' | 'LABORATORY' | 'UNIFORM' |
                          'BOOKS' | 'OTHER';

export type FeeTerm = 'TERM_1' | 'TERM_2' | 'TERM_3' | 'ANNUAL';

export interface Fee {
  id: number;
  category: FeeCategory;
  name: string;
  amount: number;
  academicYear: string;
  term?: FeeTerm;
  description?: string;
  mandatory: boolean;
  active: boolean;
  applicableClasses: ClassSummary[];
}

export interface CreateFeeRequest {
  category: FeeCategory;
  name: string;
  amount: number;
  academicYear: string;
  term?: FeeTerm;
  description?: string;
  mandatory: boolean;
  applicableClassIds?: number[];
}

export interface AssignFeeRequest {
  feeId: number;
  studentIds?: number[];
  classIds?: number[];
  dueDate: string;
  discountAmount?: number;
  discountReason?: string;
}

export interface StudentFee {
  id: number;
  feeName: string;
  category: FeeCategory;
  academicYear: string;
  dueDate: string;
  amount: number;
  discountAmount: number;
  discountReason?: string;
  netAmount: number;
  amountPaid: number;
  balance: number;
  status: 'PENDING' | 'PARTIAL' | 'PAID' | 'OVERDUE' | 'WAIVED';
  payments: PaymentSummary[];
}
```

### 7.4 Payment Models
```typescript
// models/payment.model.ts
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'MOBILE_MONEY_MTN' |
                            'MOBILE_MONEY_AIRTEL' | 'MOBILE_MONEY_ZAMTEL' |
                            'VISA' | 'MASTERCARD' | 'CHEQUE';

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' |
                            'FAILED' | 'CANCELLED' | 'REFUNDED';

export interface Payment {
  id: number;
  amount: number;
  paymentMethod: PaymentMethod;
  transactionRef: string;
  gatewayRef?: string;
  status: PaymentStatus;
  paidAt?: string;
  payerName?: string;
  payerPhone?: string;
  payerEmail?: string;
  notes?: string;
  failureReason?: string;
  student: StudentInfo;
  fee: FeeInfo;
}

export interface CreatePaymentRequest {
  studentFeeAssignmentId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  payerName?: string;
  payerPhone?: string;
  payerEmail?: string;
  notes?: string;
}

export interface PaymentSummary {
  id: number;
  amount: number;
  paymentMethod: string;
  transactionRef: string;
  status: string;
  paidAt?: string;
}
```

### 7.5 Grade & Homework Models
```typescript
// models/academic.model.ts
export type Term = 'TERM_1' | 'TERM_2' | 'TERM_3' | 'FINAL';

export interface Grade {
  id: number;
  score: number;
  maxScore: number;
  percentage: number;
  gradeLetter: string;
  term: Term;
  academicYear: string;
  teacherComment?: string;
  subject: SubjectInfo;
  student: StudentInfo;
  gradedBy?: TeacherInfo;
}

export interface CreateGradeRequest {
  enrollmentId: number;
  subjectId: number;
  score: number;
  maxScore: number;
  term: Term;
  academicYear: string;
  teacherComment?: string;
}

export interface Homework {
  id: number;
  title: string;
  description: string;
  dueDate: string;
  attachments: string[];
  maxScore?: number;
  status: 'DRAFT' | 'ACTIVE' | 'CLOSED' | 'GRADED';
  academicYear: string;
  term?: Term;
  overdue: boolean;
  subject: SubjectInfo;
  schoolClass: ClassInfo;
  teacher: TeacherInfo;
}

export interface CreateHomeworkRequest {
  subjectId: number;
  classId: number;
  title: string;
  description: string;
  dueDate: string;
  attachments?: string[];
  maxScore?: number;
  academicYear: string;
  term?: Term;
}
```

### 7.6 Pagination Model
```typescript
// models/pagination.model.ts
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}
```

---

## 8. UI/UX SPECIFICATIONS

### 8.1 Design System
- **Primary Color:** #1976D2 (Blue)
- **Secondary Color:** #388E3C (Green)
- **Accent Color:** #FFC107 (Amber)
- **Error Color:** #D32F2F (Red)
- **Warning Color:** #F57C00 (Orange)
- **Success Color:** #388E3C (Green)
- **Background:** #F5F5F5 (Light Gray)
- **Surface:** #FFFFFF (White)
- **Text Primary:** #212121
- **Text Secondary:** #757575

### 8.2 Typography
- **Font Family:** 'Roboto', sans-serif
- **Headings:**
  - H1: 32px, weight 500
  - H2: 24px, weight 500
  - H3: 20px, weight 500
  - H4: 18px, weight 500
- **Body:** 14px, weight 400
- **Caption:** 12px, weight 400

### 8.3 Spacing System
Use multiples of 8px: 8, 16, 24, 32, 48, 64

### 8.4 Responsive Breakpoints
- **Mobile:** < 600px
- **Tablet:** 600px - 959px
- **Desktop:** 960px - 1279px
- **Large Desktop:** >= 1280px

---

## 9. PAGE SPECIFICATIONS

### 9.1 Login Page
**Route:** `/auth/login`

**Features:**
- Email and password fields with validation
- "Remember me" checkbox
- "Forgot password?" link
- "Register" link
- Form validation messages
- Loading state during login
- Error message display for failed login

**Validation Rules:**
- Email: Required, valid email format
- Password: Required, minimum 8 characters

### 9.2 Admin Dashboard
**Route:** `/admin/dashboard`

**Features:**
- Welcome message with user name
- Statistics cards:
  - Total Students
  - Total Teachers
  - Total Fees Collected (Current Term)
  - Outstanding Fees
- Recent payments table (last 10)
- Fee collection chart (monthly)
- Quick action buttons:
  - Add Student
  - Create Fee
  - View Reports

### 9.3 Student Management (Admin)
**Route:** `/admin/students`

**Features:**
- Data table with columns:
  - Student ID
  - Name
  - Class
  - Parent
  - Status
  - Actions (View, Edit, Delete)
- Search bar
- Filter by: Class, Status
- Pagination
- "Add Student" button
- Export to CSV/Excel

**Student Form:**
- Personal information section
- Contact information section
- Parent selection (dropdown with search)
- Class assignment
- Medical information (optional)

### 9.4 Fee Management (Admin)
**Route:** `/admin/fees`

**Features:**
- Tabs: Fee Structures | Fee Assignments | Collection Report
- Fee structures table with:
  - Name
  - Category
  - Amount
  - Term
  - Applicable Classes
  - Actions
- Assign fee dialog with:
  - Fee selection
  - Target selection (Individual students or Classes)
  - Due date
  - Optional discount

### 9.5 Parent Dashboard
**Route:** `/parent/dashboard`

**Features:**
- Children cards showing:
  - Photo (placeholder)
  - Name
  - Class
  - Quick fee summary
- Outstanding fees alert banner
- Recent payments
- Quick pay button
- Notifications panel

### 9.6 Payment Page (Parent)
**Route:** `/parent/payments/make-payment`

**Features:**
- Select child (if multiple)
- Display outstanding fees list
- Select fees to pay (checkbox)
- Calculate total
- Payment method selection:
  - Mobile Money (MTN, Airtel, Zamtel)
  - Card (Visa, Mastercard)
  - Bank Transfer
- Enter payment details based on method
- Confirm and pay button
- Payment confirmation/receipt

### 9.7 Teacher Dashboard
**Route:** `/teacher/dashboard`

**Features:**
- Assigned classes list
- Today's schedule (optional)
- Pending homework submissions count
- Quick grade entry link
- Announcements

### 9.8 Gradebook (Teacher)
**Route:** `/teacher/gradebook`

**Features:**
- Class selector dropdown
- Subject selector dropdown
- Term selector
- Student grades table (editable cells)
- Bulk save functionality
- Grade statistics (class average, highest, lowest)

---

## 10. FORM VALIDATION RULES

### 10.1 Student Form
```typescript
{
  email: [Validators.required, Validators.email],
  firstName: [Validators.required, Validators.minLength(2), Validators.maxLength(50)],
  lastName: [Validators.required, Validators.minLength(2), Validators.maxLength(50)],
  phone: [Validators.pattern(/^\+?[0-9]{10,15}$/)],
  dateOfBirth: [Validators.required, pastDateValidator],
  gender: [Validators.required],
  address: [Validators.maxLength(500)]
}
```

### 10.2 Fee Form
```typescript
{
  name: [Validators.required, Validators.minLength(3), Validators.maxLength(100)],
  category: [Validators.required],
  amount: [Validators.required, Validators.min(0.01)],
  academicYear: [Validators.required, Validators.pattern(/^\d{4}$/)],
  term: [],
  description: [Validators.maxLength(500)],
  applicableClassIds: [Validators.required, Validators.minLength(1)]
}
```

### 10.3 Payment Form
```typescript
{
  studentFeeAssignmentId: [Validators.required],
  amount: [Validators.required, Validators.min(1)],
  paymentMethod: [Validators.required],
  payerPhone: [Validators.required, Validators.pattern(/^\+?[0-9]{10,15}$/)]
}
```

---

## 11. ERROR HANDLING

### 11.1 Global Error Handler
Create a global error interceptor that:
- Catches HTTP errors
- Displays appropriate toast/snackbar messages
- Logs errors to console (dev) or error service (prod)
- Handles specific status codes:
  - 400: Show validation errors
  - 401: Redirect to login
  - 403: Show "Access Denied" message
  - 404: Show "Not Found" message
  - 500: Show generic server error message

### 11.2 Error Message Display
```typescript
// Error response format from backend
interface ErrorResponse {
  status: number;
  message: string;
  path: string;
  timestamp: string;
}

interface ValidationErrorResponse {
  status: number;
  message: string;
  errors: { [field: string]: string };
  path: string;
  timestamp: string;
}
```

---

## 12. STATE MANAGEMENT (NgRx or Signals)

### 12.1 Auth State
```typescript
interface AuthState {
  user: User | null;
  accessToken: string | null;
  isLoading: boolean;
  error: string | null;
}
```

### 12.2 Student State (Admin)
```typescript
interface StudentState {
  students: Student[];
  selectedStudent: Student | null;
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  filters: {
    search: string;
    classId: number | null;
    status: string | null;
  };
  isLoading: boolean;
  error: string | null;
}
```

---

## 13. NOTIFICATIONS & TOASTS

### 13.1 Toast Messages
Display toast notifications for:
- Successful operations (green)
- Errors (red)
- Warnings (orange)
- Info messages (blue)

Position: Top-right
Duration: 5 seconds (auto-dismiss)

### 13.2 Notification Bell
- Show notification count badge
- Dropdown with recent notifications
- Mark as read functionality
- "View all" link

---

## 14. LOADING STATES

### 14.1 Page Loading
- Full-page spinner overlay for initial loads
- Skeleton loaders for content areas

### 14.2 Button Loading
- Disable button during operation
- Show spinner inside button
- Prevent double-clicks

### 14.3 Table Loading
- Show loading row or overlay
- Maintain table structure

---

## 15. TESTING REQUIREMENTS

### 15.1 Unit Tests
- All services should have unit tests
- All components should have basic tests
- Test coverage target: 80%

### 15.2 E2E Tests (Cypress/Playwright)
- Login flow
- Student CRUD operations
- Fee creation and assignment
- Payment flow

---

## 16. DEPLOYMENT CONFIGURATION

### 16.1 Environment Variables
```typescript
// environment.ts (development)
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  tokenRefreshThreshold: 300000 // 5 minutes before expiry
};

// environment.prod.ts (production)
export const environment = {
  production: true,
  apiUrl: 'https://api.eduflow.com/api',
  tokenRefreshThreshold: 300000
};
```

### 16.2 Build Commands
```bash
# Development
ng serve

# Production build
ng build --configuration=production

# Run tests
ng test

# Run e2e tests
ng e2e
```

---

## 17. IMPLEMENTATION PRIORITY

### Phase 1: Core (Week 1-2)
1. Project setup and configuration
2. Authentication module (login, logout, token management)
3. Core services and interceptors
4. Shared components (header, sidebar, data-table)
5. Admin layout

### Phase 2: Admin Module (Week 2-3)
1. Admin dashboard
2. Student management (CRUD)
3. Fee management
4. Fee assignment

### Phase 3: Parent Module (Week 3-4)
1. Parent dashboard
2. Children view
3. Fee viewing
4. Payment initiation

### Phase 4: Teacher Module (Week 4-5)
1. Teacher dashboard
2. Class management
3. Gradebook
4. Homework management

### Phase 5: Polish & Testing (Week 5-6)
1. Student module (read-only portal)
2. UI/UX improvements
3. Testing
4. Bug fixes

---

## 18. SAMPLE API CALLS

### 18.1 Login
```typescript
// Request
POST /v1/auth/login
Content-Type: application/json

{
  "email": "admin@eduflow.com",
  "password": "admin123"
}

// Response (200 OK)
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "email": "admin@eduflow.com",
    "firstName": "System",
    "lastName": "Administrator",
    "fullName": "System Administrator",
    "roles": ["ADMIN"]
  }
}
```

### 18.2 Create Student
```typescript
// Request
POST /v1/admin/students
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+260971234567",
  "dateOfBirth": "2010-05-15",
  "gender": "MALE",
  "address": "123 Main Street, Lusaka",
  "classId": 1,
  "parentId": 2
}

// Response (200 OK)
{
  "id": 1,
  "studentId": "STU20240001",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  ...
}
```

### 18.3 Create Fee
```typescript
// Request
POST /v1/admin/fees
Authorization: Bearer <token>
Content-Type: application/json

{
  "category": "TUITION",
  "name": "Term 1 Tuition Fee 2024",
  "amount": 5000.00,
  "academicYear": "2024",
  "term": "TERM_1",
  "description": "Tuition fee for Term 1, 2024",
  "mandatory": true,
  "applicableClassIds": [1, 2, 3]
}
```

### 18.4 Make Payment
```typescript
// Request
POST /v1/parent/payments
Authorization: Bearer <token>
Content-Type: application/json

{
  "studentFeeAssignmentId": 1,
  "amount": 2500.00,
  "paymentMethod": "MOBILE_MONEY_MTN",
  "payerName": "Jane Doe",
  "payerPhone": "+260971234567"
}

// Response (200 OK)
{
  "id": 1,
  "amount": 2500.00,
  "paymentMethod": "MOBILE_MONEY_MTN",
  "transactionRef": "PAY17234567890ABC123",
  "gatewayRef": "GW1234567890AB",
  "status": "PROCESSING",
  ...
}
```

---

## 19. SECURITY CONSIDERATIONS

1. **Never store access tokens in localStorage** - Use memory or httpOnly cookies
2. **Implement CSRF protection** if using cookies
3. **Sanitize user inputs** to prevent XSS
4. **Use HTTPS** in production
5. **Implement rate limiting** awareness (handle 429 responses)
6. **Log out user on security events** (password change, suspicious activity)

---

## 20. ACCESSIBILITY (A11Y)

1. All interactive elements must be keyboard accessible
2. Use proper ARIA labels
3. Maintain color contrast ratio of at least 4.5:1
4. Provide alt text for images
5. Use semantic HTML elements
6. Support screen readers

---

## 21. DEFAULT TEST CREDENTIALS

| Role    | Email                  | Password  |
|---------|------------------------|-----------|
| Admin   | admin@eduflow.com      | admin123  |

---

*Document Version: 1.0*
*Last Updated: February 2024*
*Backend API Version: 1.0.0*
