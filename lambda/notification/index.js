const { GoogleAuth } = require('google-auth-library');

const auth = new GoogleAuth({
  credentials: JSON.parse(process.env.GOOGLE_SERVICE_ACCOUNT_KEY),
  scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
});

const FCM_URL = `https://fcm.googleapis.com/v1/projects/${process.env.FCM_PROJECT_ID}/messages:send`;

exports.handler = async (event) => {
  const client = await auth.getClient();
  const { token } = await client.getAccessToken();

  const results = await Promise.allSettled(
    event.Records.map(async (record) => {
      const { fcmToken, title, body, data } = JSON.parse(record.body);

      const res = await fetch(FCM_URL, {
        method: 'POST',
        signal: AbortSignal.timeout(8000),
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message: {
            token: fcmToken,
            data: {
              title,
              body,
              ...(data ?? {}),
            },
            android: {
              priority: 'high',
            },
          },
        }),
      });

      if (!res.ok) {
        const json = await res.json();
        const errorCode = json.error?.details?.[0]?.errorCode ?? json.error?.status;

        const maskedToken =
          fcmToken && fcmToken.length > 8
            ? `${fcmToken.slice(0, 4)}...${fcmToken.slice(-4)}`
            : '****';

        const isInvalidToken =
          errorCode === 'UNREGISTERED' ||
          (errorCode === 'INVALID_ARGUMENT' &&
            /registration token|not a valid fcm registration token/i.test(
              json.error?.message ?? ''
            ));

        if (isInvalidToken) {
          console.warn(`FCM 토큰 무효 (스킵): ${maskedToken}, errorCode: ${errorCode}`);
          return;
        }

        console.error(`FCM 오류 응답: ${JSON.stringify(json)}`);
        throw new Error(`FCM 오류: ${json.error?.message}`);
      }
    })
  );

  const batchItemFailures = results
    .map((r, i) =>
      r.status === 'rejected'
        ? { itemIdentifier: event.Records[i].messageId }
        : null
    )
    .filter(Boolean);

  if (batchItemFailures.length > 0) {
    console.error(`실패한 메시지 수: ${batchItemFailures.length}`);
  }

  return { batchItemFailures };
};