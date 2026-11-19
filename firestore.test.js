const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_admin_1";
const BOB_UID = "bob_admin_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated access is denied across all ERP collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const collections = [
    "settings",
    "classes",
    "class_subjects",
    "teachers",
    "students",
    "fee_records",
    "attendance",
    "exams",
    "exam_schedules",
    "exam_marks",
    "timetables",
    "homeworks",
    "notices",
  ];
  for (const col of collections) {
    await assertFails(
      unauthDb.collection("users").doc(ALICE_UID).collection(col).get()
    );
  }
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("students")
      .doc("stu_1")
      .set({
        userId: ALICE_UID,
        name: "Aarav Sharma",
        fatherName: "Rajesh Sharma",
        classId: "cls_1",
        className: "Class 1",
        rollNumber: "1",
        admissionNumber: "ANV-001",
        mobile: "9876543210",
        createdAt: pastDate(),
        updatedAt: pastDate(),
      })
  );
});

test("Cross-user isolation prevents Bob from reading or modifying Alice school data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("students")
      .doc("stu_1")
      .set({
        userId: ALICE_UID,
        name: "Aarav Sharma",
        fatherName: "Rajesh Sharma",
        classId: "cls_1",
        className: "Class 1",
        rollNumber: "1",
        admissionNumber: "ANV-001",
        mobile: "9876543210",
        createdAt: pastDate(10000),
        updatedAt: pastDate(10000),
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("students").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("students")
      .doc("stu_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("students")
      .doc("stu_1")
      .update({ name: "Hijacked", updatedAt: pastDate() })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("students")
      .doc("stu_1")
      .delete()
  );
});

test("Settings, Classes, Subjects, Teachers, and Students CRUD + Red Team validation", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  const root = db.collection("users").doc(ALICE_UID);
  const t0 = pastDate(10000);

  // Settings create + update
  await assertSucceeds(
    root.collection("settings").doc("school_config").set({
      userId: ALICE_UID,
      schoolName: "ANV PUBLIC SCHOOL",
      schoolAddress: "Nagaliya Mev, Gajraula, Uttar Pradesh, India",
      academicSession: "2026-2027",
      phone: "+91 9876543210",
      email: "office@anvpublicschool.in",
      affiliationNote: "Recognized Public School",
      receiptPrefix: "ANV-2627-",
      authorizedSignatory: "Principal",
      themeMode: "LIGHT",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("settings").doc("school_config").update({
      schoolName: "ANV PUBLIC SCHOOL",
      themeMode: "DARK",
      updatedAt: pastDate(1000),
    })
  );

  // Class create + update
  await assertSucceeds(
    root.collection("classes").doc("cls_nursery").set({
      userId: ALICE_UID,
      name: "Nursery",
      section: "A",
      displayOrder: 0,
      monthlyFee: 1200.0,
      classTeacherId: "tch_1",
      classTeacherName: "Sunita Verma",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Subject create
  await assertSucceeds(
    root.collection("class_subjects").doc("sub_1").set({
      userId: ALICE_UID,
      classId: "cls_nursery",
      className: "Nursery",
      subjectName: "English",
      teacherId: "tch_1",
      teacherName: "Sunita Verma",
      defaultMaxMarks: 100,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Teacher create
  await assertSucceeds(
    root.collection("teachers").doc("tch_1").set({
      userId: ALICE_UID,
      name: "Sunita Verma",
      qualification: "M.A., B.Ed.",
      mobile: "9811122233",
      address: "Gajraula, UP",
      email: "sunita@anv.edu",
      joiningDate: "2026-04-01",
      assignedClasses: ["Nursery", "LKG"],
      assignedSubjects: ["English", "Hindi"],
      salary: 24000.0,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Student create + update
  await assertSucceeds(
    root.collection("students").doc("stu_1").set({
      userId: ALICE_UID,
      name: "Aarav Kumar",
      fatherName: "Mukesh Kumar",
      motherName: "Kavita Devi",
      dob: "2021-05-14",
      gender: "Male",
      classId: "cls_nursery",
      className: "Nursery",
      rollNumber: "1",
      admissionNumber: "ANV-26-001",
      mobile: "9988776655",
      address: "Nagaliya Mev, Gajraula",
      admissionDate: "2026-04-05",
      previousSchool: "None",
      accountNumber: "SBIN0001234",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("students").doc("stu_1").update({
      name: "Aarav Kumar Sharma",
      updatedAt: pastDate(1000),
    })
  );

  // Red team: Ghost field rejected
  await assertFails(
    root.collection("students").doc("stu_2").set({
      userId: ALICE_UID,
      name: "Test",
      fatherName: "Father",
      classId: "cls_nursery",
      className: "Nursery",
      rollNumber: "2",
      admissionNumber: "ANV-26-002",
      mobile: "9988776655",
      ghostAdmin: true,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Red team: Immutable createdAt mutation rejected
  await assertFails(
    root.collection("students").doc("stu_1").update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Red team: Future timestamp rejected
  await assertFails(
    root.collection("classes").doc("cls_future").set({
      userId: ALICE_UID,
      name: "Class 1",
      displayOrder: 3,
      monthlyFee: 1500.0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Fees, Attendance, Exams, Schedules, Marks, Timetable, Homework, and Notices CRUD", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  const root = db.collection("users").doc(ALICE_UID);
  const t0 = pastDate(10000);

  await assertSucceeds(
    root.collection("fee_records").doc("fee_1").set({
      userId: ALICE_UID,
      studentId: "stu_1",
      studentName: "Aarav Kumar",
      classId: "cls_nursery",
      className: "Nursery",
      rollNumber: "1",
      admissionNumber: "ANV-26-001",
      receiptNumber: "ANV-2627-0001",
      feeMonths: ["April 2026", "May 2026", "June 2026"],
      cycleLabel: "Q1 (Apr-Jun)",
      totalAmount: 3600.0,
      paidAmount: 3000.0,
      remainingBalance: 600.0,
      paymentDate: "2026-04-10",
      paymentMode: "Cash",
      status: "PARTIAL",
      remarks: "First installment",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("attendance").doc("2026-11-19_stu_1").set({
      userId: ALICE_UID,
      studentId: "stu_1",
      studentName: "Aarav Kumar",
      rollNumber: "1",
      classId: "cls_nursery",
      className: "Nursery",
      date: "2026-11-19",
      monthKey: "2026-11",
      status: "PRESENT",
      remarks: "",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("exams").doc("exam_1").set({
      userId: ALICE_UID,
      name: "Half Yearly Exam 2026-27",
      classId: "cls_nursery",
      className: "Nursery",
      academicSession: "2026-2027",
      startDate: "2026-11-25",
      endDate: "2026-12-05",
      status: "UPCOMING",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("exam_schedules").doc("sch_1").set({
      userId: ALICE_UID,
      examId: "exam_1",
      examName: "Half Yearly Exam 2026-27",
      classId: "cls_nursery",
      className: "Nursery",
      subjectName: "English",
      examDate: "2026-11-25",
      examDay: "Wednesday",
      timing: "09:00 AM - 11:30 AM",
      maxMarks: 100,
      passMarks: 33,
      syllabus: "Alphabet A-Z, Rhymes, Phonics",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("exam_marks").doc("mark_1").set({
      userId: ALICE_UID,
      examId: "exam_1",
      examName: "Half Yearly Exam 2026-27",
      classId: "cls_nursery",
      className: "Nursery",
      studentId: "stu_1",
      studentName: "Aarav Kumar",
      rollNumber: "1",
      scheduleId: "sch_1",
      subjectName: "English",
      maxMarks: 100,
      obtainedMarks: 92.5,
      absent: false,
      remarks: "Excellent",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("timetables").doc("tt_1").set({
      userId: ALICE_UID,
      classId: "cls_nursery",
      className: "Nursery",
      dayOfWeek: "Monday",
      periodNumber: 1,
      startTime: "08:30 AM",
      endTime: "09:15 AM",
      subjectName: "English",
      teacherId: "tch_1",
      teacherName: "Sunita Verma",
      roomNumber: "101",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("homeworks").doc("hw_1").set({
      userId: ALICE_UID,
      classId: "cls_nursery",
      className: "Nursery",
      subjectName: "English",
      title: "Practice Cursive A-E",
      description: "Write 2 pages in English notebook.",
      assignedDate: "2026-11-19",
      submissionDate: "2026-11-20",
      teacherName: "Sunita Verma",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  await assertSucceeds(
    root.collection("notices").doc("nt_1").set({
      userId: ALICE_UID,
      title: "Winter Timing Update",
      description: "School timing from Dec 1 will be 9:00 AM to 2:30 PM.",
      targetClasses: ["All Classes"],
      publicationDate: "2026-11-19",
      priority: "IMPORTANT",
      createdAt: t0,
      updatedAt: t0,
    })
  );
});
