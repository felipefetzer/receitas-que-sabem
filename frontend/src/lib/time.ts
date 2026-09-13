/**
 * R9 e R10: acima de 60 minutos mostra-se em horas e minutos;
 * se algum passo não tiver tempo, o total é aproximado.
 */
export function formatDuration(totalMinutes: number, partial: boolean): string | null {
  if (totalMinutes <= 0) return null;

  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  let text: string;
  if (hours === 0) {
    text = `${minutes} min`;
  } else if (minutes === 0) {
    text = hours === 1 ? "1 hora" : `${hours} horas`;
  } else {
    text = `${hours}h${String(minutes).padStart(2, "0")}`;
  }

  return partial ? `aproximadamente ${text}` : text;
}

export function formatIngredient(
  quantity: string | null,
  unit: string | null,
  item: string,
): string {
  return [quantity, unit, item].filter((p) => p && p.trim()).join(" ");
}
