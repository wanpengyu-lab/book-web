import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** shadcn 惯用类名合并 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
