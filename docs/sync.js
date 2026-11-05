// Google sign-in + online sync (Firebase). Does nothing until firebase-config.js has a config.
const cfg = window.FIREBASE_CONFIG;
if (cfg) {
  const V = "10.12.2";
  const [{ initializeApp }, A, F] = await Promise.all([
    import(`https://www.gstatic.com/firebasejs/${V}/firebase-app.js`),
    import(`https://www.gstatic.com/firebasejs/${V}/firebase-auth.js`),
    import(`https://www.gstatic.com/firebasejs/${V}/firebase-firestore.js`),
  ]);
  const app = initializeApp(cfg);
  const auth = A.getAuth(app);
  const db = F.getFirestore(app);
  window.SYNC_READY = true;
  let unsub = null, timer = null, applying = false;
  window.spSignIn = () => A.signInWithPopup(auth, new A.GoogleAuthProvider()).catch((e) => alert(e.message));
  window.spSignOut = () => A.signOut(auth);
  A.onAuthStateChanged(auth, (user) => {
    window.spUser(user);
    if (unsub) { unsub(); unsub = null; }
    window.spSetPush(() => {});
    if (!user) return;
    const ref = F.doc(db, "planner", user.uid);
    unsub = F.onSnapshot(ref, (snap) => {
      const mine = window.spGet();
      if (snap.exists()) {
        const remote = JSON.parse(snap.data().json);
        if (remote.updated > mine.updated) window.spApply(remote);
        else if (remote.updated < mine.updated) F.setDoc(ref, { json: JSON.stringify(mine) }, { merge: true });
      } else {
        F.setDoc(ref, { json: JSON.stringify(mine) }, { merge: true });
      }
    });
    window.spSetPush(() => {
      clearTimeout(timer);
      timer = setTimeout(() => F.setDoc(ref, { json: JSON.stringify(window.spGet()) }, { merge: true }), 600);
    });
  });
}
