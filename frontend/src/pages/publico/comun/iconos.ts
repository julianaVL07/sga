import {
  AirVent,
  Bath,
  Car,
  Check,
  CookingPot,
  Dumbbell,
  Refrigerator,
  ShowerHead,
  Sofa,
  Tv,
  Waves,
  WashingMachine,
  Wifi,
  WindArrowDown,
  type LucideIcon,
} from 'lucide-react'

const PATRONES: [RegExp, LucideIcon][] = [
  [/wi-?fi|internet/i, Wifi],
  [/aire|climatiz/i, AirVent],
  [/hidromasaje|jacuzzi|tina|ba[ñn]era/i, Bath],
  [/cocina|estufa|horno|microondas/i, CookingPot],
  [/nevera|refrigerador/i, Refrigerator],
  [/tv|televis|smart/i, Tv],
  [/vista|mar|balc[oó]n|terraza|piscina/i, Waves],
  [/lavadora|secadora|lavander/i, WashingMachine],
  [/parqueadero|garaje|estacionamiento/i, Car],
  [/ducha|ba[ñn]o/i, ShowerHead],
  [/gimnasio/i, Dumbbell],
  [/ventilador/i, WindArrowDown],
  [/sof[aá]|sala/i, Sofa],
]

/** Ícono para un elemento de la dotación según su texto */
export function iconoDotacion(elemento: string): LucideIcon {
  return PATRONES.find(([patron]) => patron.test(elemento))?.[1] ?? Check
}
