-- V16: avatar de usuario en MinIO.
--
-- Cada usuario puede tener una sola foto de perfil. Guardamos el `object_key`
-- en lugar de la URL porque las URLs presignadas tienen TTL — el backend
-- regenera la URL fresca cada vez que el cliente pide su perfil.
--
-- key format: "users/{user_id}/avatar.jpg" — siempre el mismo nombre por user,
-- asi al re-subir sobrescribe (no se acumulan archivos viejos en el bucket).
ALTER TABLE app_user ADD COLUMN avatar_object_key TEXT;
