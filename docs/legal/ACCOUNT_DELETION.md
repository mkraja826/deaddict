# Delete a DeAddict account and associated data

DeAddict supports optional accounts for synchronization and restoration. Users who never create an account can delete local recovery information from the You area of the app or by clearing the app's storage/uninstalling it.

This document is the content source for the public HTTPS deletion page that must be linked from the DeAddict Google Play listing before production publication.

## Delete from the app

1. Open DeAddict and unlock it if biometric protection is enabled.
2. Open **You**.
3. Find the account and data controls.
4. Choose **Delete account**.
5. Review the warning and confirm deletion.

The app sends an authenticated deletion request for the signed-in account and associated synchronized recovery records. The user should also choose **Delete local recovery data** on any other device where local-only information remains.

## Request deletion without the app

Use the public support channel shown on the DeAddict Google Play listing and clearly state that the request is for **DeAddict account deletion**. Include only the email address used to sign in and any information reasonably required to verify ownership. Do not include private recovery notes, passwords, payment-card details, or unnecessary health information.

A support operator must verify that the requester controls the account before processing the request. Requests should be handled without unnecessary delay and must not require the user to reinstall the app.

## Information deleted

After a verified request, DeAddict deletes the authentication account and synchronized DeAddict records associated with it, including eligible Recovery Tracks, goals, daily check-ins, tracking records, Rescue records, and synchronized preferences. Private notes are designed not to be synchronized and must be removed from each device by deleting local data or uninstalling the app.

## Information that may remain

Limited records may be retained only where required for security, fraud prevention, dispute handling, purchase verification, or legal obligations. Google Play may retain purchase and transaction records under Google's own policies. Backups or service-provider deletion queues may take additional time to expire, but deleted information must not be restored for ordinary product use.

## Guest and local-only users

A guest profile is stored locally and is not an online app account. Use **Delete local recovery data**, clear Android app storage, or uninstall DeAddict to remove guest information from that device.

## Contact

The public support email and support website shown on the DeAddict Google Play listing are the official deletion-request channels. The deployed deletion page must identify DeAddict by name, provide a clear request path, and remain accessible without requiring the user to sign in or reinstall the application.
